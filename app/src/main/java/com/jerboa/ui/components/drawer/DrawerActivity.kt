package com.jerboa.ui.components.drawer

import androidx.activity.compose.BackHandler
import androidx.compose.material3.DrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.jerboa.api.ApiState
import com.jerboa.closeDrawer
import com.jerboa.db.entity.isAnon
import com.jerboa.db.entity.isReady
import com.jerboa.feat.BlurNSFW
import com.jerboa.model.AccountViewModel
import com.jerboa.model.HomeViewModel
import com.jerboa.model.SiteViewModel
import com.jerboa.ui.components.common.getCurrentAccount
import com.jerboa.ui.components.home.NavTab
import it.vercruysse.lemmyapi.v0x19.datatypes.CommunityFollowerView
import it.vercruysse.lemmyapi.v0x19.datatypes.CommunityId
import kotlinx.coroutines.CoroutineScope

@Composable
fun MainDrawer(
    siteViewModel: SiteViewModel,
    accountViewModel: AccountViewModel,
    homeViewModel: HomeViewModel,
    scope: CoroutineScope,
    drawerState: DrawerState,
    onSettingsClick: () -> Unit,
    onCommunityClick: (CommunityId) -> Unit,
    onClickLogin: () -> Unit,
    onSelectTab: (NavTab) -> Unit,
    blurNSFW: BlurNSFW,
    showBottomNav: Boolean,
) {
    val account = getCurrentAccount(accountViewModel)

    var follows by remember { mutableStateOf(listOf<CommunityFollowerView>()) }

    BackHandler(drawerState.isOpen) {
        closeDrawer(scope, drawerState)
    }

    Drawer(
        myUserInfo =
            when (val res = siteViewModel.siteRes) {
                is ApiState.Success -> {
                    // JWT Failed
                    if (!account.isAnon() && account.isReady() && res.data.my_user == null) {
                        accountViewModel.invalidateAccount(account)
                    }
                    follows = res.data.my_user?.follows?.sortedBy { it.community.title }.orEmpty()
                    res.data.my_user
                }
                is ApiState.Failure -> {
                    // Invalidate account
                    if (account.isReady()) {
                        accountViewModel.invalidateAccount(account)
                    }

                    null
                }
                else -> null
            },
        follows = follows.toList(),
        unreadCount = siteViewModel.unreadCount,
        accountViewModel = accountViewModel,
        onAddAccount = onClickLogin,
        isOpen = drawerState.isOpen,
        onSwitchAccountClick = { acct ->
            accountViewModel.updateCurrent(acct).invokeOnCompletion {
                onSelectTab(NavTab.Home)
                closeDrawer(scope, drawerState)
            }
        },
        onSignOutClick = {
            accountViewModel.deleteAccountAndSwapCurrent(account).invokeOnCompletion {
                onSelectTab(NavTab.Home)
                closeDrawer(scope, drawerState)
            }
        },
        onSwitchAnon = {
            if (!account.isAnon()) {
                accountViewModel.removeCurrent(true).invokeOnCompletion {
                    onSelectTab(NavTab.Home)
                    closeDrawer(scope, drawerState)
                }
            }
        },
        onClickListingType = { listingType ->
            homeViewModel.updateListingType(listingType)
            homeViewModel.resetPosts()
            closeDrawer(scope, drawerState)
        },
        onCommunityClick = { community ->
            onCommunityClick(community.id)
            closeDrawer(scope, drawerState)
        },
        onClickSettings = {
            onSettingsClick()
            closeDrawer(scope, drawerState)
        },
        blurNSFW = blurNSFW,
        showBottomNav = showBottomNav,
        onSelectTab = onSelectTab,
        closeDrawer = { closeDrawer(scope, drawerState) },
    )
}
