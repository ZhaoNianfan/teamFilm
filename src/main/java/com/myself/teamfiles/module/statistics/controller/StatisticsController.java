package com.myself.teamfiles.module.statistics.controller;

import com.myself.teamfiles.common.result.R;
import com.myself.teamfiles.module.statistics.dto.AdminStatsVO;
import com.myself.teamfiles.module.statistics.dto.DashboardVO;
import com.myself.teamfiles.module.statistics.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/statistics")
@RequiredArgsConstructor
public class StatisticsController {

    private final StatisticsService statisticsService;

    @GetMapping("/dashboard")
    public R<DashboardVO> dashboard(@RequestParam(defaultValue = "PERSONAL") String space) {
        return R.ok(statisticsService.dashboard(space));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @GetMapping("/admin/overview")
    public R<AdminStatsVO> adminOverview() {
        return R.ok(statisticsService.adminOverview());
    }
}
