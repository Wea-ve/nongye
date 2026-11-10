package org.example.nongye.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ConsumerAccount {
    private Long id;
    private String nickname;
    private String phone;
    private String openId;
    /** DEFAULT 'FOLLOWER' */
    private String trustLevel;
    private String inviteCode;
    /** farmer / dealer / leader / admin */
    private String inviterType;
    private Long inviterId;
    private String profileJson;
    /** DEFAULT 1 */
    private Integer status;
    private LocalDateTime lastActiveTime;
    private LocalDateTime createTime;
}
