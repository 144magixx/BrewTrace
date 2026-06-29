package com.minyuwei.xhs.coffeeagent.user.api;

import com.minyuwei.xhs.coffeeagent.shared.api.ApiResponse;
import com.minyuwei.xhs.coffeeagent.shared.api.RequestIdFilter;
import com.minyuwei.xhs.coffeeagent.user.domain.UserPreference;

import java.util.List;

public class UserPreferenceController {
    private final RequestIdFilter requestIdFilter = new RequestIdFilter();

    public ApiResponse<List<UserPreference>> listCandidates(String requestId, List<UserPreference> preferences) {
        return ApiResponse.success(requestIdFilter.begin(requestId), preferences);
    }
}
