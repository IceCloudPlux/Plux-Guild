package com.guild.api;

import java.util.UUID;

/**
 * 公会操作回调接口
 * <p>
 * 用于异步操作的完成通知，支持成功/失败两种结果。
 * 配合 {@link GuildAPI} 的异步方法使用：
 * <pre>
 *   api.createGuildAsync("MyGuild", player)
 *      .thenAccept(guild -&gt; {
 *          if (guild != null) {
 *              player.sendMessage("创建成功: " + guild.getName());
 *          } else {
 *              player.sendMessage("创建失败");
 *          }
 *      });
 * </pre>
 *
 * @param <T> 操作结果类型
 * @since 3.1.0
 */
@FunctionalInterface
public interface GuildAPICallback<T> {

    /**
     * 操作完成时调用
     *
     * @param success 是否成功
     * @param result  操作结果，失败时可能为 null
     * @param error   错误信息，成功时为 null
     */
    void onComplete(boolean success, T result, String error);
}
