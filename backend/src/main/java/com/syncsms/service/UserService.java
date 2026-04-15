package com.syncsms.service;

import com.syncsms.dto.UserCreateRequest;
import com.syncsms.dto.UserUpdateRequest;
import com.syncsms.entity.SysUser;

import java.util.List;

/**
 * @description 用户管理服务接口
 */
public interface UserService {

    List<SysUser> listAll();

    SysUser create(UserCreateRequest request);

    SysUser update(Long id, UserUpdateRequest request);

    void delete(Long id);
}
