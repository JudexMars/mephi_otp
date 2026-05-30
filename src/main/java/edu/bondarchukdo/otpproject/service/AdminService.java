package edu.bondarchukdo.otpproject.service;

import java.util.List;

import edu.bondarchukdo.otpproject.dao.OtpConfigDao;
import edu.bondarchukdo.otpproject.dao.UserDao;
import edu.bondarchukdo.otpproject.domain.UserRecord;
import edu.bondarchukdo.otpproject.service.model.OtpConfigCommand;
import edu.bondarchukdo.otpproject.service.model.UserSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {

    private final UserDao userDao;
    private final OtpConfigDao otpConfigDao;

    public AdminService(UserDao userDao, OtpConfigDao otpConfigDao) {
        this.userDao = userDao;
        this.otpConfigDao = otpConfigDao;
    }

    private static UserSummary toSummary(UserRecord user) {
        return new UserSummary(
                user.id(), user.login(), user.role(), user.email(), user.phone(), user.telegramChatId());
    }

    public void updateOtpConfig(OtpConfigCommand command) {
        otpConfigDao.upsert(command.ttlSeconds(), command.codeLength());
    }

    public List<UserSummary> listNonAdminUsers() {
        return userDao.findAllNonAdmins().stream().map(AdminService::toSummary).toList();
    }

    @Transactional
    public void deleteUser(long id) {
        if (userDao.isAdmin(id)) {
            throw new IllegalArgumentException("Cannot delete administrator via this API");
        }
        int deleted = userDao.deleteById(id);
        if (deleted == 0) {
            throw new IllegalArgumentException("User not found");
        }
    }
}
