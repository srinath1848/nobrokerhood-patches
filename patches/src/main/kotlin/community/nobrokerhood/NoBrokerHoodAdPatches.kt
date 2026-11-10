package community.nobrokerhood

/**
 * NoBrokerHood 3.6.11 (versionCode 712) / community patch source v0.8 (sidebar experiment).
 *
 * - Carries forward ten v0.5 patches.
 * - Replaces the unsafe v0.6 unconditional default-browser redirect hook
 *   with a narrowly filtered browser launch (known ad host blocked).
 * - Adds an independent full-screen permission reminder suppression patch.
 * - Replaces the ineffective admin FAB hook with targeted promotional card suppression.
 * - Does not rewrite push delivery, channels, approval actions, or tokens.
 * - Does not claim to selectively filter the mixed announcement carousel.
 *
 * These edits are NOT compiled or device tested in this environment.
 */

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly

/**
 * NoBrokerHood 3.6.11 (versionCode 712) experimental community patches.
 * Fingerprints were checked against the user-supplied 3.6.11 XAPK.
 * NOT compiled or runtime-tested on a device for this revision.
 *
 * Security-sensitive limitation:
 * - No patches to Firebase token creation/registration or message delivery.
 * - No patches to DoorPush.hasBanner or DisplayEntryBannerFragment.onViewCreated.
 * - No attempted spoof of notification token diagnostics.
 *
 * Test the post-approval ad patch separately from the homepage patches.
 * Keep all experimental patches off until notification delivery is verified.
 */
private val NOBROKERHOOD_3611 = Compatibility(
    name = "NoBrokerHood",
    packageName = "com.app.nobrokerhood",
    targets = listOf(AppTarget(version = "3.6.11")),
)

/** Visitor carousel only; does not change the visitor approval workflow. */
@Suppress("unused")
val hideVisitorAdsPatch = bytecodePatch(
    name = "NoBrokerHood: Hide visitor-flow ads",
    description = "Hide the visitor-flow promotional carousel (3.6.11).",
    default = true,
) {
    compatibleWith(NOBROKERHOOD_3611)
    execute {
        Fingerprint(
            definingClass = "Lcom/app/nobrokerhood/features/visitorflow/presentation/composables/AdBannerKt;",
            name = "AdBannerSection",
            returnType = "V",
            parameters = listOf(
                "Lcom/app/nobrokerhood/features/visitorflow/presentation/VisitorFlowViewModel;",
                "Ljava/lang/Boolean;", "Ljava/lang/Boolean;", "Ljava/lang/String;",
                "Lcom/app/nobrokerhood/features/visitorflow/presentation/UiState;",
                "Lkotlin/jvm/functions/Function1;", "Li3/r;", "I", "I",
            ),
        ).matchSingle().method.returnEarly()
    }
}

/** Forum/feed advertisements, not the homepage top billboard. */
@Suppress("unused")
val hideHomeFeedAdsPatch = bytecodePatch(
    name = "NoBrokerHood: Hide feed ad cards",
    description = "Hide forum/home-feed ad cards (3.6.11).",
    default = true,
) {
    compatibleWith(NOBROKERHOOD_3611)
    execute {
        Fingerprint(
            definingClass = "Lcom/app/nobrokerhood/features/home_feed/ui/HomeFeedAdBannerKt;",
            name = "HomeFeedAdBanner",
            returnType = "V",
            parameters = listOf(
                "Lc4/m;",
                "Lcom/app/nobrokerhood/features/forum_new/home/model/FeedAdCard;",
                "Z", "Z", "Lkotlin/jvm/functions/Function1;",
                "Lkotlin/jvm/functions/Function3;",
                "Lkotlin/jvm/functions/Function3;",
                "Z", "Li3/r;", "I", "I",
            ),
        ).matchSingle().method.returnEarly()
    }
}

/** App-owned advertisement popup, not the genuine visit-approval UI. */
@Suppress("unused")
val hidePromotionDialogsPatch = bytecodePatch(
    name = "NoBrokerHood: Hide promotional popups",
    description = "Suppress app-level marketing dialogs (3.6.11).",
    default = true,
) {
    compatibleWith(NOBROKERHOOD_3611)
    execute {
        Fingerprint(
            definingClass = "Lcom/app/nobrokerhood/activities/ya;",
            name = "showAdvertisement",
            returnType = "V",
            parameters = listOf(
                "Lcom/app/nobrokerhood/newnobrokerhood/promotions/data/PromotionsResponse;"
            ),
        ).matchSingle().method.returnEarly()
    }
}

/**
 * EXPERIMENTAL: Hide the ad card shown AFTER visitor approval/pre-approval.
 *
 * Traced in 3.6.11:
 * PreApprovalBannerActivityNew.R1 obtains
 * PostApprovalCardResponse.getTickets().firstOrNull() and passes the
 * optional card to PreApprovalBannerScreen. That screen checks for null
 * before rendering the ad card. Returning an empty list hides that card
 * without skipping the separate success/visitor UI or approval action.
 *
 * This does not modify push notifications, token registration or the
 * actual approval request. Runtime validation is still required.
 */
@Suppress("unused")
val hidePostApprovalAdsPatch = bytecodePatch(
    name = "NoBrokerHood: Hide post-approval ad card (experimental)",
    description = "Hide the sponsored card shown after entry approval or pre-approval (3.6.11).",
    default = false,
) {
    compatibleWith(NOBROKERHOOD_3611)
    execute {
        Fingerprint(
            definingClass =
                "Lcom/app/nobrokerhood/newnobrokerhood/postApprovalCard/data/model/PostApprovalCardResponse;",
            name = "getTickets",
            returnType = "Ljava/util/List;",
            parameters = emptyList(),
        ).matchSingle().method.addInstructions(
            0,
            """
            invoke-static {}, Ljava/util/Collections;->emptyList()Ljava/util/List;
            move-result-object v0
            return-object v0
            """.trimIndent(),
        )
    }
}

/**
 * EXPERIMENTAL top homepage ad state. Return an empty ad list rather than null;
 * also stop displaying the skeleton/loading and expansion state.
 * The surrounding layout may still reserve space; runtime testing required.
 */
@Suppress("unused")
val hideHomepageTopAdsPatch = bytecodePatch(
    name = "NoBrokerHood: Hide homepage top ads (experimental)",
    description = "Supply an empty top-ad list and disable its loading/expanded state.",
    default = false,
) {
    compatibleWith(NOBROKERHOOD_3611)
    execute {
        val model = "Lcom/app/nobrokerhood/features/homepage/data/remote/models/TopAdState;"
        Fingerprint(
            definingClass = model,
            name = "getList",
            returnType = "Ljava/util/List;",
            parameters = emptyList(),
        ).matchSingle().method.addInstructions(
            0,
            """
            invoke-static {}, Ljava/util/Collections;->emptyList()Ljava/util/List;
            move-result-object v0
            return-object v0
            """.trimIndent(),
        )
        Fingerprint(
            definingClass = model,
            name = "isLoading",
            returnType = "Z",
            parameters = emptyList(),
        ).matchSingle().method.returnEarly(false)
        Fingerprint(
            definingClass = model,
            name = "isExpanded",
            returnType = "Z",
            parameters = emptyList(),
        ).matchSingle().method.returnEarly(false)
    }
}

/**
 * EXPERIMENTAL: secondary app-bar promotional banner state.
 * Only modifies the advertising model; not push, FCM or entry/exit logic.
 */
@Suppress("unused")
val hideAppBarBannersPatch = bytecodePatch(
    name = "NoBrokerHood: Hide app-bar banners (experimental)",
    description = "Empty app-bar banner entries; may leave a layout placeholder.",
    default = false,
) {
    compatibleWith(NOBROKERHOOD_3611)
    execute {
        val model = "Lcom/app/nobrokerhood/features/app_bar_banner/data/models/BannerState;"
        Fingerprint(
            definingClass = model,
            name = "getList",
            returnType = "Ljava/util/List;",
            parameters = emptyList(),
        ).matchSingle().method.addInstructions(
            0,
            """
            invoke-static {}, Ljava/util/Collections;->emptyList()Ljava/util/List;
            move-result-object v0
            return-object v0
            """.trimIndent(),
        )
        Fingerprint(
            definingClass = model,
            name = "isLoading",
            returnType = "Z",
            parameters = emptyList(),
        ).matchSingle().method.returnEarly(false)
        Fingerprint(
            definingClass = model,
            name = "isExpanded",
            returnType = "Z",
            parameters = emptyList(),
        ).matchSingle().method.returnEarly(false)
    }
}


/**
 * EXPERIMENTAL: Remove the large image from expanded Android notifications.
 *
 * In 3.6.11, DoorFirebaseMessagingService.J reads the "bigImageUrl" payload
 * and calls U(...) to build a NotificationCompat Style. U's THIRD String
 * argument (p3) controls its image-loading/BigPictureStyle branch; when empty,
 * the existing function falls back to BigTextStyle.
 *
 * This patch clears only that third String argument (p3) at the start of U(...),
 * allowing the original code to construct and post its notification.
 * It does NOT block incoming Firebase messages, modify registration tokens,
 * disable any notification channel, or suppress the notification builder.
 *
 * LIMITATION: Any other notification using U(...) to show a legitimate
 * large image will also lose its expanded picture. Notification text and
 * delivery still need testing on a real device.
 */
@Suppress("unused")
val hideExpandedNotificationImagesPatch = bytecodePatch(
    name = "NoBrokerHood: Hide expanded notification images (experimental)",
    description = "Hide large banner images in entry/exit notifications while retaining text (3.6.11).",
    default = false,
) {
    compatibleWith(NOBROKERHOOD_3611)
    execute {
        Fingerprint(
            definingClass =
                "Lcom/app/nobrokerhood/notifications/DoorFirebaseMessagingService;",
            name = "U",
            returnType = "Landroidx/core/app/o\$j;",
            parameters = listOf(
                "Ljava/lang/String;",
                "Ljava/lang/String;",
                "Ljava/lang/String;",
                "Z",
                "Ljava/lang/String;",
            ),
        ).matchSingle().method.addInstructions(0, "const-string p3, \"\"")
    }
}


/**
 * EXPERIMENTAL: Suppress the fallback promotional card after approval.
 *
 * The original 7-patch build empties PostApprovalCardResponse.getTickets(),
 * but DisplayEntryBannerFragment also consults getDefaultCard().
 * Returning null is intended to remove that fallback. A caller that does
 * not tolerate null may crash. Test ONLY this new patch first and retain
 * the previous working patched APK for rollback.
 */
@Suppress("unused")
val hidePostApprovalFallbackCardPatch = bytecodePatch(
    name = "NoBrokerHood: Hide default post-approval card (experimental)",
    description = "Hide the fallback ad card after entry approval. Null handling is untested.",
    default = false,
) {
    compatibleWith(NOBROKERHOOD_3611)
    execute {
        Fingerprint(
            definingClass =
                "Lcom/app/nobrokerhood/newnobrokerhood/postApprovalCard/data/model/PostApprovalCardResponse;",
            name = "getDefaultCard",
            returnType =
                "Lcom/app/nobrokerhood/newnobrokerhood/postApprovalCard/data/model/PostApprovalCardResponseData;",
            parameters = emptyList(),
        ).matchSingle().method.addInstructions(
            0,
            """
            const/4 v0, 0x0
            return-object v0
            """.trimIndent(),
        )
    }
}

/**
 * EXPERIMENTAL: Remove cards at the app-bar banner response boundary.
 *
 * The existing patch empties BannerState.getList(), but the app also has
 * AppBarBannerResponseData.getCards(). The actual homepage screen has
 * still displayed a top banner. Empty this upstream list as a separate
 * hypothesis, not as a guaranteed fix. It may leave a blank container.
 */
@Suppress("unused")
val hideAppBarResponseCardsPatch = bytecodePatch(
    name = "NoBrokerHood: Hide app-bar response cards (experimental)",
    description = "Remove app-bar advertising card data before creating banner state.",
    default = false,
) {
    compatibleWith(NOBROKERHOOD_3611)
    execute {
        Fingerprint(
            definingClass =
                "Lcom/app/nobrokerhood/features/app_bar_banner/data/models/AppBarBannerResponseData;",
            name = "getCards",
            returnType = "Ljava/util/List;",
            parameters = emptyList(),
        ).matchSingle().method.addInstructions(
            0,
            """
            invoke-static {}, Ljava/util/Collections;->emptyList()Ljava/util/List;
            move-result-object v0
            return-object v0
            """.trimIndent(),
        )
    }
}

/**
 * EXPERIMENTAL: Hide the home-discover carousel's cards.
 *
 * DiscoverData.getTickets() returns ArrayList, NOT List. We must return a
 * real ArrayList so callers that expect an ArrayList do not class-cast.
 * This may also hide non-ad promotional/discover items in that carousel.
 */
@Suppress("unused")
val hideHomeDiscoverCarouselPatch = bytecodePatch(
    name = "NoBrokerHood: Hide home-discover carousel (experimental)",
    description = "Hide cards in the homepage discover carousel; may hide non-ad items too.",
    default = false,
) {
    compatibleWith(NOBROKERHOOD_3611)
    execute {
        Fingerprint(
            definingClass =
                "Lcom/app/nobrokerhood/newnobrokerhood/home_native_discover_carousel/models/DiscoverData;",
            name = "getTickets",
            returnType = "Ljava/util/ArrayList;",
            parameters = emptyList(),
        ).matchSingle().method.addInstructions(
            0,
            """
            new-instance v0, Ljava/util/ArrayList;
            invoke-direct {v0}, Ljava/util/ArrayList;-><init>()V
            return-object v0
            """.trimIndent(),
        )
    }
}


/**
 * EXPERIMENTAL (v0.7): Open ordinary links with the Android default browser,
 * but refuse to launch URLs hosted on the observed advertising redirect
 * domain (apps.multipl.in / multipl.in).
 *
 * This deliberately replaces the v0.6 catch-all ACTION_VIEW hook, which
 * opened the promotional link when the notification was tapped.
 *
 * IMPORTANT: this is a *known-host filter*, not a comprehensive ad blocker.
 * The server can change campaign hosts. It does not rewrite PendingIntents,
 * visitor approvals, FCM registration, or notification delivery.
 *
 * Original 3.6.11 signature:
 *   Lcom/app/nobrokerhood/utilities/t1;->R7(Activity, int, String): void
 * Method has 4 free local registers (v0..v3), so the injected code uses
 * v0..v2 only and preserves p1/p2/p3.
 */
@Suppress("unused")
val useDefaultBrowserInsteadOfChromePatch = bytecodePatch(
    name = "NoBrokerHood: Default browser, block known ad hosts (experimental)",
    description = "Use Android browser for genuine links; block known Multipl redirect hosts.",
    default = false,
) {
    compatibleWith(NOBROKERHOOD_3611)
    execute {
        Fingerprint(
            definingClass = "Lcom/app/nobrokerhood/utilities/t1;",
            name = "R7",
            returnType = "V",
            parameters = listOf("Landroid/app/Activity;", "I", "Ljava/lang/String;"),
        ).matchSingle().method.addInstructionsWithLabels(
            0,
            """
            if-eqz p3, :blocked
            invoke-static {p3}, Landroid/net/Uri;->parse(Ljava/lang/String;)Landroid/net/Uri;
            move-result-object v2
            invoke-virtual {v2}, Landroid/net/Uri;->getHost()Ljava/lang/String;
            move-result-object v2
            const-string v0, "apps.multipl.in"
            invoke-virtual {v0, v2}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z
            move-result v0
            if-nez v0, :blocked
            const-string v0, "multipl.in"
            invoke-virtual {v0, v2}, Ljava/lang/String;->equalsIgnoreCase(Ljava/lang/String;)Z
            move-result v0
            if-nez v0, :blocked
            new-instance v0, Landroid/content/Intent;
            const-string v1, "android.intent.action.VIEW"
            invoke-static {p3}, Landroid/net/Uri;->parse(Ljava/lang/String;)Landroid/net/Uri;
            move-result-object v2
            invoke-direct {v0, v1, v2}, Landroid/content/Intent;-><init>(Ljava/lang/String;Landroid/net/Uri;)V
            const-string v1, "android.intent.category.BROWSABLE"
            invoke-virtual {v0, v1}, Landroid/content/Intent;->addCategory(Ljava/lang/String;)Landroid/content/Intent;
            invoke-virtual {p1, v0, p2}, Landroid/app/Activity;->startActivityForResult(Landroid/content/Intent;I)V
            return-void
            :blocked
            return-void
            """.trimIndent(),
        )
    }
}

/**
 * v0.7: Suppress the HOME-screen re-prompt asking to enable full-screen
 * visitor alerts. The call site HomeActivity.m8() solely creates and shows
 * FullScreenPermissionDialog using the tag FullScreenPermissionSheetDialog.
 *
 * Suppressing this method does NOT grant/revoke notification permission,
 * change user preferences, or stop regular visitor notifications.
 */
@Suppress("unused")
val suppressFullscreenReminderPatch = bytecodePatch(
    name = "NoBrokerHood: Stop full-screen permission nag",
    description = "Stop the home-screen full-screen alert reminder without changing permissions.",
    default = false,
) {
    compatibleWith(NOBROKERHOOD_3611)
    execute {
        Fingerprint(
            definingClass = "Lcom/app/nobrokerhood/activities/HomeActivity;",
            name = "m8",
            returnType = "V",
            parameters = emptyList(),
        ).matchSingle().method.returnEarly()
    }
}

/**
 * EXPERIMENTAL (v0.8): Remove draggable promotional picture-in-picture
 * card, likely responsible for the Philips/iPhone/Malabar side tab.
 *
 * This is NOT HomeActivity.c8 (which gates the admin floating button).
 * The app's base activity ya.showMultimediaCard(PromotionData) inflates
 * multimediaCardView and sets up a draggable sponsored visual.
 *
 * This hook only skips building that promotional card. It does not alter
 * notifications, visitor approvals, homepage top banners or legitimate
 * browser links. Actual sidebar removal remains a device-test hypothesis.
 */
@Suppress("unused")
val suppressFloatingCampaignWidgetPatch = bytecodePatch(
    name = "NoBrokerHood: Hide promotional side tab (experimental)",
    description = "Suppress the draggable multimedia promotion card; verify on-device.",
    default = false,
) {
    compatibleWith(NOBROKERHOOD_3611)
    execute {
        Fingerprint(
            definingClass = "Lcom/app/nobrokerhood/activities/ya;",
            name = "showMultimediaCard",
            returnType = "V",
            parameters = listOf(
                "Lcom/app/nobrokerhood/newnobrokerhood/promotions/data/PromotionData;",
            ),
        ).matchSingle().method.returnEarly()
    }
}
