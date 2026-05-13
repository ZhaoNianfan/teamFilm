package com.myself.teamfiles.module.recycle.controller;

import com.myself.teamfiles.common.annotation.OperationLog;
import com.myself.teamfiles.common.result.PageResult;
import com.myself.teamfiles.common.result.R;
import com.myself.teamfiles.module.recycle.dto.RecyclePageDTO;
import com.myself.teamfiles.module.recycle.dto.RecycleVO;
import com.myself.teamfiles.module.recycle.service.RecycleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/recycle")
@RequiredArgsConstructor
public class RecycleController {

    private final RecycleService recycleService;

    @GetMapping
    public R<PageResult<RecycleVO>> list(RecyclePageDTO dto) {
        return R.ok(recycleService.list(dto));
    }

    @OperationLog(module = "RECYCLE", operation = "Restore from recycle bin")
    @PostMapping("/{id}/restore")
    public R<Void> restore(@PathVariable Long id) {
        recycleService.restore(id);
        return R.ok();
    }

    @OperationLog(module = "RECYCLE", operation = "Permanently delete from recycle bin")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        recycleService.delete(id);
        return R.ok();
    }

    @OperationLog(module = "RECYCLE", operation = "Batch permanently delete from recycle bin")
    @DeleteMapping("/batch")
    public R<Void> batchDelete(@RequestBody List<Long> ids) {
        recycleService.batchDelete(ids);
        return R.ok();
    }

    @OperationLog(module = "RECYCLE", operation = "Clear recycle bin")
    @DeleteMapping("/clear")
    public R<Void> clear(@RequestParam(value = "storageSpace", required = false) String storageSpace) {
        recycleService.clear(storageSpace);
        return R.ok();
    }
}
