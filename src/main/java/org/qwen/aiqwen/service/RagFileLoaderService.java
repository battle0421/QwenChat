package org.qwen.aiqwen.service;

public interface RagFileLoaderService {
    /**
     * 加载文件接口
     * @param path
     */
    public void loadRagFile(String path);


    /**
     * 搜索相似的文本片段
     * @param query 查询文本
     * @param maxResults 最大返回结果数
     * @return 匹配的文本片段列表
     */
    public String searchSimilar(String query, int maxResults);
}
