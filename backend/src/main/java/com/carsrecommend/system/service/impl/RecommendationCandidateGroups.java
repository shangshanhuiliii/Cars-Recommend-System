package com.carsrecommend.system.service.impl;

import java.util.List;

record RecommendationCandidateGroups(
        List<RecommendationCandidate> strictCandidates,
        List<RecommendationCandidate> recommendationCandidates) {
}
