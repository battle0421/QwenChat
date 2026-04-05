package org.qwen.aiqwen.service;

import org.qwen.aiqwen.dto.PersonDto;

public interface RagFileLoaderService {
    /**
     * 加载文件接口
     * @param path
     */
    public void loadRagFile(String path);
    /**
     * 提取人名信息
     *
     */
    public PersonDto extractPerson(String memoryId, String message);


    /**
     * 搜索相似的文本片段
     * @param query 查询文本
     * @param maxResults 最大返回结果数
     * @return 匹配的文本片段列表
     */
    public String searchSimilar(String memoryId ,String query, int maxResults);

    /**
     * 判断是否为好的消息
     * @param message 输入消息
     * @return 是否为好的消息
     */
    public Boolean isGoodFlag(String message);
}
