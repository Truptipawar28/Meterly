package com.meterly.service.cost;

import com.meterly.domain.Metric;

/**
 * One row of the aggregation query: total quantity of one metric on one endpoint.
 * Filled directly by JPQL "select new com.meterly.service.cost.UsageTotal(...)".
 */
public record UsageTotal(Long serviceId, String serviceName, Long endpointId, String endpointName,
                         Metric metric, Long quantity) {
}
