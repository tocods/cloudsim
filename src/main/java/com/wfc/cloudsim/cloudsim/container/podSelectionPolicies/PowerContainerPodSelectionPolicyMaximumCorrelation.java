package com.wfc.cloudsim.cloudsim.container.podSelectionPolicies;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import com.wfc.cloudsim.cloudsim.container.core.*;
import com.wfc.cloudsim.cloudsim.util.MathUtil;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by sareh on 3/08/15.
 */
public class PowerContainerPodSelectionPolicyMaximumCorrelation extends PowerContainerPodSelectionPolicy {


        /** The fallback policy. */
        private PowerContainerPodSelectionPolicy fallbackPolicy;

        /**
         * Instantiates a new power vm selection policy maximum correlation.
         *
         * @param fallbackPolicy the fallback policy
         */
        public PowerContainerPodSelectionPolicyMaximumCorrelation(final PowerContainerPodSelectionPolicy fallbackPolicy) {
            super();
            setFallbackPolicy(fallbackPolicy);
        }

        /*
         * (non-Javadoc)
         *
         * @see com.wfc.cloudsim.cloudsim.experiments.power.PowerPodSelectionPolicy#
         * getVmsToMigrate(com.wfc.cloudsim .cloudsim.power.PowerHost)
         */
        @Override
        public ContainerPod getVmToMigrate(final PowerContainerHost host) {
            List<PowerContainerPod> migratableVms = getMigratableVms(host);
            if (migratableVms.isEmpty()) {
                return null;
            }
            List<Double> metrics = null;
            try {
                metrics = getCorrelationCoefficients(getUtilizationMatrix(migratableVms));
            } catch (IllegalArgumentException e) { // the degrees of freedom must be greater than zero
                return getFallbackPolicy().getVmToMigrate(host);
            }
            double maxMetric = Double.MIN_VALUE;
            int maxIndex = 0;
            for (int i = 0; i < metrics.size(); i++) {
                double metric = metrics.get(i);
                if (metric > maxMetric) {
                    maxMetric = metric;
                    maxIndex = i;
                }
            }
            return migratableVms.get(maxIndex);
        }

        /**
         * Gets the utilization matrix.
         *
         * @param vmList the host
         * @return the utilization matrix
         */
        protected double[][] getUtilizationMatrix(final List<PowerContainerPod> vmList) {
            int n = vmList.size();
            int m = getMinUtilizationHistorySize(vmList);
            double[][] utilization = new double[n][m];
            for (int i = 0; i < n; i++) {
                List<Double> vmUtilization = vmList.get(i).getUtilizationHistory();
                for (int j = 0; j < vmUtilization.size(); j++) {
                    utilization[i][j] = vmUtilization.get(j);
                }
            }
            return utilization;
        }

        /**
         * Gets the min utilization history size.
         *
         * @param vmList the vm list
         * @return the min utilization history size
         */
        protected int getMinUtilizationHistorySize(final List<PowerContainerPod> vmList) {
            int minSize = Integer.MAX_VALUE;
            for (PowerContainerPod vm : vmList) {
                int size = vm.getUtilizationHistory().size();
                if (size < minSize) {
                    minSize = size;
                }
            }
            return minSize;
        }

        /**
         * Gets the correlation coefficients.
         *
         * @param data the data
         * @return the correlation coefficients
         */
        protected List<Double> getCorrelationCoefficients(final double[][] data) {
            int n = data.length;
            int m = data[0].length;
            List<Double> correlationCoefficients = new LinkedList<Double>();
            for (int i = 0; i < n; i++) {
                double[][] x = new double[n - 1][m];
                int k = 0;
                for (int j = 0; j < n; j++) {
                    if (j != i) {
                        x[k++] = data[j];
                    }
                }

                // Transpose the matrix so that it fits the linear model
                double[][] xT = new Array2DRowRealMatrix(x).transpose().getData();

                // RSquare is the "coefficient of determination"
                correlationCoefficients.add(MathUtil.createLinearRegression(xT,
                        data[i]).calculateRSquared());
            }
            return correlationCoefficients;
        }

        /**
         * Gets the fallback policy.
         *
         * @return the fallback policy
         */
        public PowerContainerPodSelectionPolicy getFallbackPolicy() {
            return fallbackPolicy;
        }

        /**
         * Sets the fallback policy.
         *
         * @param fallbackPolicy the new fallback policy
         */
        public void setFallbackPolicy(final PowerContainerPodSelectionPolicy fallbackPolicy) {
            this.fallbackPolicy = fallbackPolicy;
        }

    }


