package com.socialchat.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.socialchat.common.ErrorCode;
import com.socialchat.constant.UserConstant;
import com.socialchat.dao.UserMapper;
import com.socialchat.email.EmailHelper;
import com.socialchat.exception.BusinessException;
import com.socialchat.model.entity.User;
import com.socialchat.model.request.UserLoginRequest;
import com.socialchat.model.request.UserRegisterRequest;
import com.socialchat.model.request.UserUpdateRequest;
import com.socialchat.model.session.UserSession;
import com.socialchat.model.vo.UserVO;
import com.socialchat.service.UserService;
import com.socialchat.utils.CodeGeneratorUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.List;

/**
 * (tb_user)表服务实现类
 *
 * @author 清闲
 * @since 2024-12-15 16:24:38
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private UserMapper userMapper;

    @Resource
    private EmailHelper emailHelper;

    @Override
    public Boolean getVerifyCode(String userEmail) {
        // 判断和上次生成验证码是否超过 60s 限制的 key
        String restrictKey = userEmail + UserConstant.USER_EMAIL_RESTRICT_KEY;
        // 如果还没达到一分钟
        if (stringRedisTemplate.hasKey(restrictKey)) {
            // todo 后面可以加上黑名单等措施，定时任务等等，现在简单地返回 false 代表不生成
            log.info("该用户在60s内已经发送过验证码，邮箱为{}，拒绝此次发送", userEmail);
            return false;
        }

        String verifyCode = CodeGeneratorUtil.generateCode(6);
        String encryptVerifyCode = DigestUtil.md5Hex(UserConstant.SALT + verifyCode);

        // 发送验证码 todo 这里直接发送的话高并发流量下会存在性能瓶颈，后期引入 MQ 削峰
        boolean flag;
        try {
            flag = emailHelper.sendEmail(userEmail, verifyCode);
        } catch (MessagingException e) {
            log.error("验证码发送失败，邮箱为{}", userEmail);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "验证码发送失败");
        }

        if (!flag) {
            log.error("验证码发送失败，邮箱为{}", userEmail);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "验证码发送失败");
        }

        // 将验证码存储在 redis 中，限制 key 存活时间为 60s，验证码存活时间为 300s
        stringRedisTemplate.opsForValue().set(restrictKey, userEmail, Duration.ofSeconds(60));
        stringRedisTemplate.opsForValue().set(userEmail + UserConstant.USER_EMAIL_REGISTER_PREFIX, encryptVerifyCode, Duration.ofSeconds(5 * 60));
        return true;
    }

    @Override
    public Boolean register(UserRegisterRequest userRegisterRequest) {
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String userEmail = userRegisterRequest.getUserEmail();
        String verifyCode = userRegisterRequest.getVerifyCode();
        String encryptUserPassword = DigestUtil.md5Hex(UserConstant.SALT + userPassword);

        // 先校验验证码
        String redisVerifyCode = stringRedisTemplate.opsForValue().get(userEmail + UserConstant.USER_EMAIL_REGISTER_PREFIX);
        String encryptVerifyCode = DigestUtil.md5Hex(UserConstant.SALT + verifyCode);
        // 验证码不存在或者验证码不相同
        if (StringUtils.isBlank(redisVerifyCode) || !StringUtils.equalsIgnoreCase(redisVerifyCode, encryptVerifyCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误或验证码已过期");
        }

        // 再校验账号和邮箱
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper
                .eq(StringUtils.isNotBlank(userAccount), User::getUserAccount, userAccount)
                .or()
                .eq(StringUtils.isNotBlank(userEmail), User::getUserEmail, userEmail);
        List<User> userList = userMapper.selectList(queryWrapper);
        // 如果用户名或者账号其中一个存在数据库中，则注册失败
        if (CollectionUtils.isNotEmpty(userList)) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "该账号或邮箱已存在，请勿重复注册");
        }

        // 注册新用户，插入用户信息
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptUserPassword);
        user.setUserEmail(userEmail);
        int insert = userMapper.insert(user);

        return insert > 0;
    }

    @Override
    public String login(UserLoginRequest userLoginRequest) {
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        String encryptUserPassword = DigestUtil.md5Hex(UserConstant.SALT + userPassword);
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号或密码不能为空");
        }

        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(StringUtils.isNotBlank(userAccount), User::getUserAccount, userAccount);
        queryWrapper.eq(StringUtils.isNotBlank(encryptUserPassword), User::getUserPassword, encryptUserPassword);
        User user = userMapper.selectOne(queryWrapper);
        if (user == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "该账号对应用户不存在或密码错误");
        }

        StpUtil.login(userAccount);
        UserSession userSession = new UserSession();
        BeanUtils.copyProperties(user, userSession);

        // 存储在 session 中，便于获取用户信息
        StpUtil.getTokenSession().set(UserConstant.USERINFO, userSession);
        return StpUtil.getTokenValue();
    }

    @Override
    public boolean deleteUser() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        String header = request.getHeader(UserConstant.AUTHORIZATION);
        String token = header.substring(7);

        UserSession userSession = (UserSession) StpUtil.getTokenSessionByToken(token).get(UserConstant.USERINFO);
        int delete = userMapper.deleteById(userSession.getId());
        if (delete == 0) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "用户删除失败，用户已不存在");
        }
        StpUtil.logout();
        return true;
    }

    @Override
    public UserVO getLoginUser() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        String header = request.getHeader(UserConstant.AUTHORIZATION);
        String token = header.substring(7);

        UserSession userSession = (UserSession) StpUtil.getTokenSessionByToken(token).get(UserConstant.USERINFO);
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(userSession, userVO);
        return userVO;
    }

    @Override
    public UserSession updateUserInfo(UserUpdateRequest request) {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        HttpServletRequest httpServletRequest = ((ServletRequestAttributes) requestAttributes).getRequest();
        String header = httpServletRequest.getHeader(UserConstant.AUTHORIZATION);
        String token = header.substring(7);

        UserSession userSession = (UserSession) StpUtil.getTokenSessionByToken(token).get(UserConstant.USERINFO);
        Long userId = userSession.getId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "用户不存在");
        }

        String userName = request.getUserName();
        String userProfile = request.getUserProfile();

        if (StringUtils.isNotBlank(userName)) {
            user.setUserName(userName);
        }
        if (StringUtils.isNotBlank(userProfile)) {
            user.setUserProfile(userProfile);
        }
        userMapper.updateById(user);

        // 存储在 session 中，便于获取用户信息
        BeanUtils.copyProperties(user, userSession);
        StpUtil.getTokenSessionByToken(token).set(UserConstant.USERINFO, userSession);

        return userSession;
    }

    @Override
    public UserSession updateUserAvatar(String userAvatar) {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        HttpServletRequest httpServletRequest = ((ServletRequestAttributes) requestAttributes).getRequest();
        String header = httpServletRequest.getHeader(UserConstant.AUTHORIZATION);
        String token = header.substring(7);

        UserSession userSession = (UserSession) StpUtil.getTokenSessionByToken(token).get(UserConstant.USERINFO);
        Long userId = userSession.getId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "用户不存在");
        }
        user.setUserAvatar(userAvatar);
        userMapper.updateById(user);

        // 存储在 session 中，便于获取用户信息
        BeanUtils.copyProperties(user, userSession);
        StpUtil.getTokenSessionByToken(token).set(UserConstant.USERINFO, userSession);

        return userSession;
    }

}
