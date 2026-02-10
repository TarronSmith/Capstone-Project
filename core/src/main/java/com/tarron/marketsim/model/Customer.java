package com.tarron.marketsim.model;

public class Customer {
	String purchaseStrat;
	double budget;
	
	public Customer(String purchaseStrat, double budget) {
		this.purchaseStrat = purchaseStrat;
		this.budget = budget;
	}
	
	public String getSpendingType() {
		return purchaseStrat;
	}
	
	public double getWallet() {
		return budget;
	}
	
	public boolean spend(double amount) {
	    if (amount < 0) return false;
	    if (budget < amount) return false;
	    budget -= amount;
	    return true;
	}
}
