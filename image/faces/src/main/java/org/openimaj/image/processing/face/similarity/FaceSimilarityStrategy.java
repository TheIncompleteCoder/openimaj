package org.openimaj.image.processing.face.similarity;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openimaj.feature.FloatFVComparison;
import org.openimaj.image.FImage;
import org.openimaj.image.Image;
import org.openimaj.image.processing.face.detection.DetectedFace;
import org.openimaj.image.processing.face.detection.FaceDetector;
import org.openimaj.image.processing.face.feature.FacePatchFeature;
import org.openimaj.image.processing.face.feature.FacialFeature;
import org.openimaj.image.processing.face.feature.FacialFeatureFactory;
import org.openimaj.image.processing.face.feature.comparison.FaceFVComparator;
import org.openimaj.image.processing.face.feature.comparison.FacialFeatureComparator;
import org.openimaj.image.processing.face.feature.ltp.ReversedLtpDtFeature;
import org.openimaj.image.processing.face.keypoints.FKEFaceDetector;
import org.openimaj.image.processing.face.keypoints.KEDetectedFace;
import org.openimaj.math.geometry.shape.Rectangle;
import org.openimaj.math.matrix.similarity.SimilarityMatrix;
import org.openimaj.math.matrix.similarity.processor.InvertData;


public class FaceSimilarityStrategy<D extends DetectedFace, F extends FacialFeature, I extends Image<?, I>> {

	private FaceDetector<D, I> detector;
	private FacialFeatureFactory<F, D> featureFactory;
	private FacialFeatureComparator<F> comparator;
	private Map<String, Rectangle> boundingBoxes;
	private LinkedHashMap<String, Map<String, Double>> similarityMatrix;
	private List<D> queryfaces;
	private List<D> testfaces;
	private String queryId;
	private String testId;

	public FaceSimilarityStrategy(FaceDetector<D, I> detector,
			FacialFeatureFactory<F, D> featureFactory,
			FacialFeatureComparator<F> comparator) {
		this.detector = detector;
		this.featureFactory = featureFactory;
		this.comparator = comparator;
		this.similarityMatrix = new LinkedHashMap<String, Map<String,Double>>();
		this.boundingBoxes = new HashMap<String, Rectangle>();
	}

	/**
	 * @return the detector
	 */
	public FaceDetector<D, I> detector() {
		return detector;
	}

	/**
	 * @return the featureFactory
	 */
	public FacialFeatureFactory<F, D> featureFactory() {
		return featureFactory;
	}

	/**
	 * @return the comparator
	 */
	public FacialFeatureComparator<F> comparator() {
		return comparator;
	}

	/**
	 * Provided for ease of generics
	 * 
	 * @param detector
	 * @param featureFactory
	 * @param comparator
	 * @return
	 */
	public static <D extends DetectedFace, F extends FacialFeature, I extends Image<?, I>> FaceSimilarityStrategy<D, F, I> build(
			FaceDetector<D, I> detector,
			FacialFeatureFactory<F, D> featureFactory,
			FacialFeatureComparator<F> comparator) {
		return new FaceSimilarityStrategy<D, F, I>(detector, featureFactory,
				comparator);
	}

//	/**
//	 * Set the place where face bounding boxes are held
//	 * 
//	 * @param boundingBoxes
//	 */
//	public void setBoundingBoxes(Map<String, Rectangle> boundingBoxes) {
//		this.boundingBoxes = boundingBoxes;
//	}

//	/**
//	 * Set the place where similarity scores between faces are held
//	 * 
//	 * @param similarityMatrix
//	 */
//	public void setSimilarityMatrix(
//			Map<String, Map<String, Double>> similarityMatrix) {
//		this.similarityMatrix = similarityMatrix;
//	}

	public void setQuery(I queryImage, String queryId) {
		this.queryfaces = this.detector.detectFaces(queryImage);
		this.queryId = queryId;
		updateBoundingBix(this.queryfaces, queryId);
	}

	private void updateBoundingBix(List<D> faces, String imageId) {
		// We need to store the first one if we're running withFirst = true
		if (boundingBoxes != null)
			for (int ff = 0; ff < faces.size(); ff++)
				if (boundingBoxes.get(imageId + ":" + ff) == null)
					boundingBoxes.put(imageId + ":" + ff, faces.get(ff)
							.getBounds());
	}

	/**
	 * Set the image against which the query will be compared to next
	 * 
	 * @param testImage
	 * @param testId
	 */
	public void setTest(I testImage, String testId) {
		this.testId = testId;
		this.testfaces = this.detector.detectFaces(testImage);
		updateBoundingBix(this.testfaces, testId);
	}

	/**
	 * Compare the query to itself for the next test
	 */
	public void setQueryTest() {
		this.testfaces = this.queryfaces;
		this.testId = this.queryId;
	}

	public void performTest() {
		// Now compare all the faces in the first image
		// with all the faces in the second image.
		for (int ii = 0; ii < queryfaces.size(); ii++) {
			String face1id = queryId + ":" + ii;
			D f1f = queryfaces.get(ii);
			F f1fv = featureFactory.createFeature(f1f, true);
			// NOTE that the distance matrix will be symmetrical
			// so we only have to do half the comparisons.
			for (int jj = 0; jj < testfaces.size(); jj++) {
				double d = 0;
				String face2id = null;

				// If we're comparing the same face in the same image
				// we can assume the distance is zero. Saves doing a match.
				if (queryfaces == testfaces && ii == jj) {
					d = 0;
					face2id = face1id;
				} else {
					// Compare the two feature vectors using the chosen
					// distance metric.
					D f2f = testfaces.get(jj);
					face2id = testId + ":" + jj;
					
					
					F f2fv = featureFactory.createFeature(f2f, false);

					d = comparator.compare(f1fv, f2fv);
				}

				// Put the result in the result map
				Map<String, Double> mm = this.similarityMatrix.get(face1id);
				if (mm == null)
					this.similarityMatrix.put(face1id, mm = new HashMap<String, Double>());
				mm.put(face2id, d);
			}
		}
	}

	/**
	 * Get the similarity dictionary structured as: {image0:face0 => {image0:face0 => DISTANCE,...},...,}
	 * @return
	 */
	public Map<String, Map<String, Double>> getSimilarityDictionary() {
		return this.similarityMatrix;
	}
	
	public SimilarityMatrix getSimilarityMatrix() {
		Set<String> keys = this.similarityMatrix.keySet();
		String[] indexArr = keys.toArray(new String[keys.size()]);
		SimilarityMatrix simMatrix = new SimilarityMatrix(indexArr);
		for (int i = 0; i < indexArr.length; i++) {
			String x = indexArr[i];
			for (int j = 0; j < indexArr.length; j++) {
				String y = indexArr[j];
				simMatrix.set(i, j, this.similarityMatrix.get(x).get(y));
			}
		}
		
		if(this.comparator.isAscending()){
			simMatrix.processInline(new InvertData());
		}
		return simMatrix;
	}

	public Map<String,Rectangle> getBoundingBoxes() {
		return this.boundingBoxes;
	}
}