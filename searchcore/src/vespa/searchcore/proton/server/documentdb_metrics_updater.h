// Copyright 2018 Yahoo Holdings. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
#pragma once

#include <vespa/searchcore/proton/metrics/documentdb_tagged_metrics.h>
#include <vespa/searchlib/docstore/cachestats.h>

namespace proton {

namespace matching { class SessionManager; }

class AttributeUsageFilter;
class DDBState;
class DocumentDBJobTrackers;
class DocumentDBMetricsCollection;
class DocumentSubDBCollection;
class ExecutorThreadingService;
class ExecutorThreadingServiceStats;
class LegacyDocumentDBMetrics;

/**
 * Class used to update metrics for a document db.
 */
class DocumentDBMetricsUpdater {
public:

    struct DocumentStoreCacheStats {
        search::CacheStats total;
        search::CacheStats readySubDb;
        search::CacheStats notReadySubDb;
        search::CacheStats removedSubDb;
        DocumentStoreCacheStats() : total(), readySubDb(), notReadySubDb(), removedSubDb() {}
    };

private:
    const DocumentSubDBCollection &_subDBs;
    ExecutorThreadingService &_writeService;
    DocumentDBJobTrackers &_jobTrackers;
    matching::SessionManager &_sessionManager;
    const AttributeUsageFilter &_writeFilter;
    const DDBState &_state;
    // Last updated document store cache statistics. Necessary due to metrics implementation is upside down.
    DocumentStoreCacheStats _lastDocStoreCacheStats;

    void updateLegacyMetrics(LegacyDocumentDBMetrics &metrics, const ExecutorThreadingServiceStats &threadingServiceStats);
    void updateMiscMetrics(DocumentDBTaggedMetrics &metrics, const ExecutorThreadingServiceStats &threadingServiceStats);
    void updateAttributeResourceUsageMetrics(DocumentDBTaggedMetrics::AttributeMetrics &metrics);

public:
    DocumentDBMetricsUpdater(const DocumentSubDBCollection &subDBs,
                             ExecutorThreadingService &writeService,
                             DocumentDBJobTrackers &jobTrackers,
                             matching::SessionManager &sessionManager,
                             const AttributeUsageFilter &writeFilter,
                             const DDBState &state);
    ~DocumentDBMetricsUpdater();

    void updateMetrics(DocumentDBMetricsCollection &metrics);

};

}
