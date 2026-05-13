package com.myself.teamfiles.module.recycle.service;

import com.myself.teamfiles.common.result.PageResult;
import com.myself.teamfiles.module.recycle.dto.RecyclePageDTO;
import com.myself.teamfiles.module.recycle.dto.RecycleVO;

public interface RecycleService {
    PageResult<RecycleVO> list(RecyclePageDTO dto);
    void restore(Long id);
    void delete(Long id);
    void batchDelete(java.util.List<Long> ids);
    void clear(String storageSpace);
}
