package edu.bondarchukdo.otpproject.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import edu.bondarchukdo.otpproject.dao.OtpConfigDao;
import edu.bondarchukdo.otpproject.dao.UserDao;
import edu.bondarchukdo.otpproject.domain.Role;
import edu.bondarchukdo.otpproject.domain.UserRecord;
import edu.bondarchukdo.otpproject.service.model.OtpConfigCommand;
import edu.bondarchukdo.otpproject.service.model.UserSummary;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserDao userDao;

    @Mock
    private OtpConfigDao otpConfigDao;

    @InjectMocks
    private AdminService adminService;

    @Test
    void updateOtpConfigDelegatesToDao() {
        adminService.updateOtpConfig(new OtpConfigCommand(120, 6));

        verify(otpConfigDao).upsert(120, 6);
    }

    @Test
    void listNonAdminUsersMapsRecords() {
        UserRecord user = new UserRecord(2L, "u1", "hash", Role.USER, "u1@example.com", null, null);
        when(userDao.findAllNonAdmins()).thenReturn(List.of(user));

        List<UserSummary> result = adminService.listNonAdminUsers();

        assertEquals(1, result.size());
        assertEquals("u1", result.get(0).login());
        assertEquals(Role.USER, result.get(0).role());
    }

    @Test
    void deleteUserRemovesNonAdmin() {
        when(userDao.isAdmin(2L)).thenReturn(false);
        when(userDao.deleteById(2L)).thenReturn(1);

        adminService.deleteUser(2L);

        verify(userDao).deleteById(2L);
    }

    @Test
    void deleteUserRejectsAdmin() {
        when(userDao.isAdmin(1L)).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> adminService.deleteUser(1L));
        assertEquals("Cannot delete administrator via this API", ex.getMessage());
    }

    @Test
    void deleteUserRejectsMissingUser() {
        when(userDao.isAdmin(99L)).thenReturn(false);
        when(userDao.deleteById(99L)).thenReturn(0);

        assertThrows(IllegalArgumentException.class, () -> adminService.deleteUser(99L));
    }
}
