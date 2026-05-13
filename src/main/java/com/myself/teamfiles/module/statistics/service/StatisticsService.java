package com.myself.teamfiles.module.statistics.service;

import com.myself.teamfiles.module.statistics.dto.AdminStatsVO;
import com.myself.teamfiles.module.statistics.dto.DashboardVO;

public interface StatisticsService {
    DashboardVO dashboard(String space);
    AdminStatsVO adminOverview();
}
