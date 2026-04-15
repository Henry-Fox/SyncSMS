package com.syncsms.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.syncsms.dto.UserCreateRequest;
import com.syncsms.dto.UserUpdateRequest;
import com.syncsms.entity.SysUser;
import com.syncsms.mapper.SysUserMapper;
import com.syncsms.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * @description 用户管理服务实现
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<SysUser> listAll() {
        List<SysUser> users = userMapper.selectList(null);
        users.forEach(u -> u.setPassword(null));
        return users;
    }

    @Override
    public SysUser create(UserCreateRequest request) {
        SysUser exists = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, request.getUsername()));
        if (exists != null) {
            throw new RuntimeException("用户名已存在");
        }

        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setRole(request.getRole());
        user.setStatus(1);
        userMapper.insert(user);

        user.setPassword(null);
        return user;
    }

    @Override
    public SysUser update(Long id, UserUpdateRequest request) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        if (StringUtils.hasText(request.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getRole() != null) {
            user.setRole(request.getRole());
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }
        userMapper.updateById(user);

        user.setPassword(null);
        return user;
    }

    @Override
    public void delete(Long id) {
        SysUser user = userMapper.selectById(id);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if ("admin".equals(user.getRole())) {
            long adminCount = userMapper.selectCount(
                    new LambdaQueryWrapper<SysUser>().eq(SysUser::getRole, "admin"));
            if (adminCount <= 1) {
                throw new RuntimeException("不能删除最后一个管理员");
            }
        }
        userMapper.deleteById(id);
    }
}
