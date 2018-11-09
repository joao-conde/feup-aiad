package main;

import utilities.Utils;

public class RatingInfo {

	private int totalWaitingAnswers;
	private int totalValidAnswers;
	private float sumOfRatings;
	
	public RatingInfo(int totalAnswers) {
		totalWaitingAnswers = totalAnswers;
		totalValidAnswers = 0;
		sumOfRatings = 0;
	}
	
	public Float calculateAverageRating() {
		if(totalWaitingAnswers != 0) 
			return null;
		else if(totalValidAnswers == 0)
				return (float) -1;
		else
			return sumOfRatings / (float)totalValidAnswers;
	}
	
	public void processRatingInfo(String rating) {
		System.out.println("DENTRO DO PROCESSRATING");
		if(!rating.equals(Utils.NULL)) {
			totalValidAnswers++;
			sumOfRatings += Float.parseFloat(rating);
		}
		if(totalWaitingAnswers > 0) totalWaitingAnswers--;
	}
	
}
