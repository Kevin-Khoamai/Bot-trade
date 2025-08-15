package com.cryptotrading.risk.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.random.MersenneTwister;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.IntStream;

/**
 * Service for Value at Risk (VaR) calculations using multiple methodologies
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VarCalculationService {

    private final MersenneTwister random = new MersenneTwister();

    /**
     * Calculate VaR using Historical Simulation method
     */
    public BigDecimal calculateHistoricalVaR(List<BigDecimal> returns, double confidenceLevel) {
        if (returns == null || returns.isEmpty()) {
            return BigDecimal.ZERO;
        }

        try {
            // Sort returns in ascending order
            List<BigDecimal> sortedReturns = new ArrayList<>(returns);
            sortedReturns.sort(BigDecimal::compareTo);

            // Calculate percentile index
            double percentile = (1.0 - confidenceLevel) * 100;
            int index = (int) Math.ceil((percentile / 100.0) * sortedReturns.size()) - 1;
            index = Math.max(0, Math.min(index, sortedReturns.size() - 1));

            BigDecimal var = sortedReturns.get(index).abs();
            
            log.debug("Historical VaR calculated: {} at {}% confidence level", var, confidenceLevel * 100);
            return var;

        } catch (Exception e) {
            log.error("Error calculating historical VaR: {}", e.getMessage(), e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Calculate VaR using Parametric (Normal Distribution) method
     */
    public BigDecimal calculateParametricVaR(List<BigDecimal> returns, double confidenceLevel) {
        if (returns == null || returns.isEmpty()) {
            return BigDecimal.ZERO;
        }

        try {
            DescriptiveStatistics stats = new DescriptiveStatistics();
            returns.forEach(r -> stats.addValue(r.doubleValue()));

            double mean = stats.getMean();
            double stdDev = stats.getStandardDeviation();

            // Get z-score for confidence level
            NormalDistribution normalDist = new NormalDistribution();
            double zScore = normalDist.inverseCumulativeProbability(1.0 - confidenceLevel);

            // Calculate VaR
            double varValue = Math.abs(mean + zScore * stdDev);
            BigDecimal var = BigDecimal.valueOf(varValue).setScale(8, RoundingMode.HALF_UP);

            log.debug("Parametric VaR calculated: {} at {}% confidence level", var, confidenceLevel * 100);
            return var;

        } catch (Exception e) {
            log.error("Error calculating parametric VaR: {}", e.getMessage(), e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Calculate VaR using Monte Carlo Simulation
     */
    public BigDecimal calculateMonteCarloVaR(List<BigDecimal> returns, double confidenceLevel, int simulations) {
        if (returns == null || returns.isEmpty()) {
            return BigDecimal.ZERO;
        }

        try {
            DescriptiveStatistics stats = new DescriptiveStatistics();
            returns.forEach(r -> stats.addValue(r.doubleValue()));

            double mean = stats.getMean();
            double stdDev = stats.getStandardDeviation();

            // Generate random scenarios
            List<Double> simulatedReturns = new ArrayList<>();
            NormalDistribution normalDist = new NormalDistribution(random, mean, stdDev);

            for (int i = 0; i < simulations; i++) {
                simulatedReturns.add(normalDist.sample());
            }

            // Sort and find VaR
            simulatedReturns.sort(Double::compareTo);
            int index = (int) Math.ceil((1.0 - confidenceLevel) * simulations) - 1;
            index = Math.max(0, Math.min(index, simulatedReturns.size() - 1));

            BigDecimal var = BigDecimal.valueOf(Math.abs(simulatedReturns.get(index)))
                    .setScale(8, RoundingMode.HALF_UP);

            log.debug("Monte Carlo VaR calculated: {} at {}% confidence level with {} simulations", 
                    var, confidenceLevel * 100, simulations);
            return var;

        } catch (Exception e) {
            log.error("Error calculating Monte Carlo VaR: {}", e.getMessage(), e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Calculate Conditional VaR (Expected Shortfall)
     */
    public BigDecimal calculateConditionalVaR(List<BigDecimal> returns, double confidenceLevel) {
        if (returns == null || returns.isEmpty()) {
            return BigDecimal.ZERO;
        }

        try {
            // Sort returns in ascending order
            List<BigDecimal> sortedReturns = new ArrayList<>(returns);
            sortedReturns.sort(BigDecimal::compareTo);

            // Find VaR threshold
            double percentile = (1.0 - confidenceLevel) * 100;
            int varIndex = (int) Math.ceil((percentile / 100.0) * sortedReturns.size()) - 1;
            varIndex = Math.max(0, Math.min(varIndex, sortedReturns.size() - 1));

            // Calculate average of returns worse than VaR
            BigDecimal sum = BigDecimal.ZERO;
            int count = 0;

            for (int i = 0; i <= varIndex; i++) {
                sum = sum.add(sortedReturns.get(i).abs());
                count++;
            }

            BigDecimal cvar = count > 0 ? sum.divide(BigDecimal.valueOf(count), 8, RoundingMode.HALF_UP) : BigDecimal.ZERO;

            log.debug("Conditional VaR calculated: {} at {}% confidence level", cvar, confidenceLevel * 100);
            return cvar;

        } catch (Exception e) {
            log.error("Error calculating conditional VaR: {}", e.getMessage(), e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Calculate portfolio VaR with correlation matrix
     */
    public BigDecimal calculatePortfolioVaR(Map<String, BigDecimal> positions, 
                                           Map<String, List<BigDecimal>> assetReturns,
                                           double[][] correlationMatrix,
                                           double confidenceLevel) {
        try {
            if (positions.isEmpty() || assetReturns.isEmpty()) {
                return BigDecimal.ZERO;
            }

            List<String> assets = new ArrayList<>(positions.keySet());
            int n = assets.size();

            // Calculate individual asset VaRs
            double[] individualVars = new double[n];
            for (int i = 0; i < n; i++) {
                String asset = assets.get(i);
                List<BigDecimal> returns = assetReturns.get(asset);
                if (returns != null && !returns.isEmpty()) {
                    BigDecimal assetVar = calculateParametricVaR(returns, confidenceLevel);
                    BigDecimal position = positions.get(asset);
                    individualVars[i] = assetVar.multiply(position).doubleValue();
                }
            }

            // Calculate portfolio VaR using correlation matrix
            double portfolioVariance = 0.0;
            for (int i = 0; i < n; i++) {
                for (int j = 0; j < n; j++) {
                    double correlation = (correlationMatrix != null && 
                                        i < correlationMatrix.length && 
                                        j < correlationMatrix[i].length) 
                                       ? correlationMatrix[i][j] : (i == j ? 1.0 : 0.0);
                    portfolioVariance += individualVars[i] * individualVars[j] * correlation;
                }
            }

            BigDecimal portfolioVaR = BigDecimal.valueOf(Math.sqrt(Math.abs(portfolioVariance)))
                    .setScale(8, RoundingMode.HALF_UP);

            log.debug("Portfolio VaR calculated: {} at {}% confidence level", portfolioVaR, confidenceLevel * 100);
            return portfolioVaR;

        } catch (Exception e) {
            log.error("Error calculating portfolio VaR: {}", e.getMessage(), e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Calculate VaR for different time horizons
     */
    public Map<String, BigDecimal> calculateMultiHorizonVaR(List<BigDecimal> returns, double confidenceLevel) {
        Map<String, BigDecimal> varResults = new HashMap<>();

        try {
            // 1-day VaR
            BigDecimal var1Day = calculateParametricVaR(returns, confidenceLevel);
            varResults.put("1D", var1Day);

            // Scale to different horizons (assuming square root of time scaling)
            varResults.put("5D", var1Day.multiply(BigDecimal.valueOf(Math.sqrt(5))).setScale(8, RoundingMode.HALF_UP));
            varResults.put("10D", var1Day.multiply(BigDecimal.valueOf(Math.sqrt(10))).setScale(8, RoundingMode.HALF_UP));
            varResults.put("20D", var1Day.multiply(BigDecimal.valueOf(Math.sqrt(20))).setScale(8, RoundingMode.HALF_UP));
            varResults.put("30D", var1Day.multiply(BigDecimal.valueOf(Math.sqrt(30))).setScale(8, RoundingMode.HALF_UP));

            log.debug("Multi-horizon VaR calculated for confidence level: {}%", confidenceLevel * 100);

        } catch (Exception e) {
            log.error("Error calculating multi-horizon VaR: {}", e.getMessage(), e);
        }

        return varResults;
    }

    /**
     * Calculate Component VaR (contribution of each position to portfolio VaR)
     */
    public Map<String, BigDecimal> calculateComponentVaR(Map<String, BigDecimal> positions,
                                                        Map<String, List<BigDecimal>> assetReturns,
                                                        double[][] correlationMatrix,
                                                        double confidenceLevel) {
        Map<String, BigDecimal> componentVars = new HashMap<>();

        try {
            BigDecimal portfolioVaR = calculatePortfolioVaR(positions, assetReturns, correlationMatrix, confidenceLevel);
            
            if (portfolioVaR.compareTo(BigDecimal.ZERO) == 0) {
                return componentVars;
            }

            List<String> assets = new ArrayList<>(positions.keySet());
            
            for (String asset : assets) {
                // Calculate VaR without this asset
                Map<String, BigDecimal> reducedPositions = new HashMap<>(positions);
                reducedPositions.remove(asset);
                
                Map<String, List<BigDecimal>> reducedReturns = new HashMap<>(assetReturns);
                reducedReturns.remove(asset);
                
                BigDecimal reducedVaR = calculatePortfolioVaR(reducedPositions, reducedReturns, correlationMatrix, confidenceLevel);
                
                // Component VaR is the difference
                BigDecimal componentVaR = portfolioVaR.subtract(reducedVaR);
                componentVars.put(asset, componentVaR);
            }

            log.debug("Component VaR calculated for {} assets", assets.size());

        } catch (Exception e) {
            log.error("Error calculating component VaR: {}", e.getMessage(), e);
        }

        return componentVars;
    }

    /**
     * Calculate Incremental VaR (impact of adding a position)
     */
    public BigDecimal calculateIncrementalVaR(Map<String, BigDecimal> currentPositions,
                                             String newAsset,
                                             BigDecimal newPosition,
                                             Map<String, List<BigDecimal>> assetReturns,
                                             double[][] correlationMatrix,
                                             double confidenceLevel) {
        try {
            // Current portfolio VaR
            BigDecimal currentVaR = calculatePortfolioVaR(currentPositions, assetReturns, correlationMatrix, confidenceLevel);

            // New portfolio with additional position
            Map<String, BigDecimal> newPositions = new HashMap<>(currentPositions);
            newPositions.put(newAsset, newPosition);

            BigDecimal newVaR = calculatePortfolioVaR(newPositions, assetReturns, correlationMatrix, confidenceLevel);

            // Incremental VaR is the difference
            BigDecimal incrementalVaR = newVaR.subtract(currentVaR);

            log.debug("Incremental VaR calculated for adding {} position in {}: {}", 
                    newPosition, newAsset, incrementalVaR);

            return incrementalVaR;

        } catch (Exception e) {
            log.error("Error calculating incremental VaR: {}", e.getMessage(), e);
            return BigDecimal.ZERO;
        }
    }

    /**
     * Perform VaR backtesting
     */
    public VarBacktestResult performVarBacktest(List<BigDecimal> historicalReturns,
                                               List<BigDecimal> varEstimates,
                                               double confidenceLevel) {
        try {
            if (historicalReturns.size() != varEstimates.size()) {
                throw new IllegalArgumentException("Historical returns and VaR estimates must have same size");
            }

            int violations = 0;
            int totalObservations = historicalReturns.size();
            List<Integer> violationDates = new ArrayList<>();

            for (int i = 0; i < totalObservations; i++) {
                BigDecimal actualLoss = historicalReturns.get(i).abs();
                BigDecimal varEstimate = varEstimates.get(i);

                if (actualLoss.compareTo(varEstimate) > 0) {
                    violations++;
                    violationDates.add(i);
                }
            }

            double violationRate = (double) violations / totalObservations;
            double expectedViolationRate = 1.0 - confidenceLevel;
            
            // Kupiec test statistic
            double kupiecStatistic = calculateKupiecStatistic(violations, totalObservations, expectedViolationRate);
            
            VarBacktestResult result = VarBacktestResult.builder()
                    .totalObservations(totalObservations)
                    .violations(violations)
                    .violationRate(violationRate)
                    .expectedViolationRate(expectedViolationRate)
                    .kupiecStatistic(kupiecStatistic)
                    .violationDates(violationDates)
                    .isModelAccurate(Math.abs(violationRate - expectedViolationRate) < 0.05) // 5% tolerance
                    .build();

            log.info("VaR backtest completed: {} violations out of {} observations ({}% vs {}% expected)",
                    violations, totalObservations, violationRate * 100, expectedViolationRate * 100);

            return result;

        } catch (Exception e) {
            log.error("Error performing VaR backtest: {}", e.getMessage(), e);
            return VarBacktestResult.builder().build();
        }
    }

    /**
     * Calculate Kupiec test statistic for VaR model validation
     */
    private double calculateKupiecStatistic(int violations, int observations, double expectedRate) {
        if (violations == 0 || violations == observations) {
            return 0.0;
        }

        double observedRate = (double) violations / observations;
        double logLikelihood1 = violations * Math.log(observedRate) + (observations - violations) * Math.log(1 - observedRate);
        double logLikelihood0 = violations * Math.log(expectedRate) + (observations - violations) * Math.log(1 - expectedRate);
        
        return -2 * (logLikelihood0 - logLikelihood1);
    }

    /**
     * VaR backtest result
     */
    @lombok.Data
    @lombok.Builder
    public static class VarBacktestResult {
        private int totalObservations;
        private int violations;
        private double violationRate;
        private double expectedViolationRate;
        private double kupiecStatistic;
        private List<Integer> violationDates;
        private boolean isModelAccurate;
    }
}
