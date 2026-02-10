package com.tarron.marketsim.model;

public class DecisionLogic {

	public Item iPick(Customer customer, Item A, Item B) {
		double priceA = A.getPrice();
		int qualityA = A.getQuality();
		double priceB = B.getPrice();
		int qualityB = B.getQuality();

		double wallet = customer.getWallet();

		String spendingType = customer.getSpendingType();

		int scoreA = 0;
		int scoreB = 0;

		// Check customer wallet to see if they have enough currency for either item.
		if(wallet < priceA && wallet < priceB) return null;
		if(wallet >= priceA && wallet < priceB) return (A);
		if(wallet < priceA && wallet >= priceB) return (B);

		// Will choose an item based on price and quality.
		if("Value Buyer".equals(spendingType)) {
			// Scoring
			if(priceA < priceB) scoreA++;
			else {
				scoreB++;
			}

			if(qualityA > qualityB) scoreA++;
			else if(qualityA < qualityB) scoreB++;

			// Compare scores
			if(scoreA > scoreB) return (A);
			else if(scoreA < scoreB) return (B); 
			else { // Scores are equal, so choose item with lowest price
				if (priceA < priceB) return A;
				else if(priceB < priceA) return B;

				// Prices are equal -> compare quality
				if(qualityA > qualityB) return A;

				else if(qualityA < qualityB) return B;

				// Still tied -> default choice
				return A;
			}
		}
		// Will choose an item solely on lowest price
		else if("Cheap Buyer".equals(spendingType)) {
			if(priceA < priceB) return A;
			else if(priceA > priceB) return B;
			else {
				if(qualityA > qualityB) return A;
				else if(qualityA < qualityB) return B;
				return A;
			}

		}
		// Will choose an item solely on highest price
		else if("Conspicuous Buyer".equals(spendingType)) {
			if(priceA > priceB) return A;
			else if(priceA < priceB) return B;
			else {
				if(qualityA > qualityB) return A;
				else if(qualityA < qualityB) return B;
				return A;
			}
		}

		else return null;
	}
}
