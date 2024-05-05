package com.hmdp.service;

import com.hmdp.dto.Result;
import com.hmdp.entity.Blog;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author XIZAI
 * @since 2021-12-22
 */
public interface IBlogService extends IService<Blog> {
    public Result queryBlogById(Long id);

    /**
     * 点赞功能实现
     * @param id
     * @return
     */
    public Result likeBlog(Long id);

    public Result queryHotBlog(Integer current);

    /**
     * 根据博客id 查询点赞排行榜
     * @param id
     * @return
     */
    public Result queryBlogLikes(Long id);

    public Result saveBlog(Blog blog);

    Result queryBlogOfFollow(Long max, Integer offset);
}
