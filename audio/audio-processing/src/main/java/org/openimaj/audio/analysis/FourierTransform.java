/**
 * Copyright (c) 2011, The University of Southampton and the individual contributors.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *   * 	Redistributions of source code must retain the above copyright notice,
 * 	this list of conditions and the following disclaimer.
 *
 *   *	Redistributions in binary form must reproduce the above copyright notice,
 * 	this list of conditions and the following disclaimer in the documentation
 * 	and/or other materials provided with the distribution.
 *
 *   *	Neither the name of the University of Southampton nor the names of its
 * 	contributors may be used to endorse or promote products derived from this
 * 	software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
/**
 * 
 */
package org.openimaj.audio.analysis;

import org.openimaj.audio.AudioFormat;
import org.openimaj.audio.SampleChunk;
import org.openimaj.audio.processor.AudioProcessor;
import org.openimaj.audio.samples.SampleBuffer;
import org.openimaj.audio.samples.SampleBufferFactory;

import edu.emory.mathcs.jtransforms.fft.FloatFFT_1D;

/**
 * 	Perform an FFT on an audio signal. An FFT will be calculated for every
 * 	channel in the audio separately. Use {@link #getLastFFT()} to get the
 * 	last generated frequency domain calculation.
 * 	<p>
 * 	The class also includes an inverse transform function that takes a
 * 	frequency domain array (such as that delivered by {@link #getLastFFT()})
 * 	and returns a {@link SampleChunk}. The format of the output sample chunk
 * 	is determined by the given audio format.
 * 
 *  @author David Dupplaw (dpd@ecs.soton.ac.uk)
 *	@created 28 Oct 2011
 */
public class FourierTransform extends AudioProcessor
{
	/** The last generated FFT */
	private float[][] lastFFT = null;
	
	/**
	 *	{@inheritDoc}
	 * 	@see org.openimaj.audio.processor.AudioProcessor#process(org.openimaj.audio.SampleChunk)
	 */
	@Override
    public SampleChunk process( SampleChunk sample )
    {
		// Get a sample buffer object for this data
		final SampleBuffer sb = sample.getSampleBuffer();
		
		// The number of channels we need to process
		final int nChannels = sample.getFormat().getNumChannels();
		
		// Number of samples we'll need to process for each channel
		final int nSamplesPerChannel = sb.size() / nChannels;
		
		// The Fourier transformer we're going to use
		final FloatFFT_1D fft = new FloatFFT_1D( nSamplesPerChannel );
		
		// Creates an FFT for each of the channels in turn
		lastFFT = new float[nChannels][];
		for( int c = 0; c < nChannels; c++ )
		{
			lastFFT[c] = new float[ nSamplesPerChannel*2 ];
			for( int x = 0; x < nSamplesPerChannel; x++ )
				lastFFT[c][x] = sb.get( x*nChannels+c )/(float)Integer.MAX_VALUE;
			
			fft.complexForward( lastFFT[c] );
		}
		
		/**
		ShortBuffer sb = sample.getSamplesAsByteBuffer().asShortBuffer();
		
		// We only use the first channel
		final int nChans = sample.getFormat().getNumChannels();
		lastFFT = new float[sample.getNumberOfSamples()/nChans*2];
		
		// Fill the FFT input with values -0.5 to 0.5 (for signed) 0 to 1 for unsigned
		final float fftscale = (float)Math.pow( 2, sample.getFormat().getNBits() );
		for( int x = 0; x < sample.getNumberOfSamples()/nChans; x++ )
			lastFFT[x*2] = sb.get( x*nChans )/fftscale;
		
		FloatFFT_1D fft = new FloatFFT_1D( sample.getNumberOfSamples()/nChans );
		fft.complexForward( lastFFT );
		**/
		
	    return sample;
    }
	
	/**
	 * 	Given some transformed audio data, will convert it back into	
	 * 	a sample chunk. The number of channels given audio format
	 * 	must match the data that is provided in the transformedData array.
	 * 
	 * 	@param format The required format for the output
	 *	@param transformedData The frequency domain data
	 *	@return A {@link SampleChunk}
	 */
	static public SampleChunk inverseTransform( AudioFormat format, 
			float[][] transformedData )
	{
		// Check the data has something in it.
		if( transformedData == null || transformedData.length == 0 )
			throw new IllegalArgumentException( "No data in data chunk" );
		
		// Check that the transformed data has the same number of channels
		// as the data we've been given.
		if( transformedData.length != format.getNumChannels() )
			throw new IllegalArgumentException( "Number of channels in audio " +
					"format does not match given data." );

		// The number of channels
		final int nChannels = transformedData.length;
		
		// The fourier transformer we're going to use
		final FloatFFT_1D fft = new FloatFFT_1D( transformedData[0].length/2 );

		// Create a sample buffer to put the time domain data into
		final SampleBuffer sb = SampleBufferFactory.createSampleBuffer( format, 
				transformedData[0].length/2 *	nChannels );
		
		// Perform the inverse on each channel
		for( int channel = 0; channel < transformedData.length; channel++ )
		{
			// Convert frequency domain back to time domain
			fft.complexInverse( transformedData[channel], true );

			// Set the data in the buffer
			for( int x = 0; x < transformedData[channel].length/2; x++ )
				sb.set( x*nChannels+channel, 
					transformedData[channel][x] * Integer.MAX_VALUE );
		}
		
		// Return a new sample chunk
		return sb.getSampleChunk();
	}
	
	/**
	 * 	Get the last processed FFT frequency data.
	 * 	@return The fft of the last processed window 
	 */
	public float[][] getLastFFT()
	{
		return this.lastFFT;
	}
}