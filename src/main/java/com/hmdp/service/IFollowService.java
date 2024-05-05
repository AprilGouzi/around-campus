package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.Follow;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author XIZAI
 * @since 2021-12-22
 */
public interface IFollowService extends IService<Follow> {
    public Result follow(Long userFollowId,boolean isFollow);

    public Result isFollow(Long userFollowId);

    public Result followCommons(Long id);

}
