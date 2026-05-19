package com.edu.live.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserInfo {
    private Long id;
    private String username;
    private String email;
    private String role;
    private Integer status;
    private String avatar;
}
