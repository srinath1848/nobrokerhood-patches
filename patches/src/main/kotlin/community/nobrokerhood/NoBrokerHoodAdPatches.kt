package community.nobrokerhood

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.AppTarget
import app.morphe.patcher.patch.Compatibility
import app.morphe.patcher.util.matchSingle
import app.morphe.patcher.util.returnEarly

/*
 * NoBrokerHood 3.6.6 / versionCode 707.
 * Fingerprint descriptors were selected after inspecting a 3.6.6 XAPK.
 * These patches have not been compiled or runtime-tested.
 * Patches only UI / banner-predicate methods; no visitor approval or entry/exit
 * notification delivery methods are changed.
 *
 * Use as an overlay in MorpheApp/morphe-patches-template.
 */

private const val DOOR_PUSH = "Lcom/app/nobrokerhood/models/DoorPush;"

// Pin every patch to the single APK version actually examined.
private val NOBROKERHOOD_366 = Compatibility(
    name = "NoBrokerHood",
    packageName = "com.app.nobrokerhood",
    targets = listOf(AppTarget(version = "3.6.6")),
)

/** Visitor-flow Jetpack Compose promotional carousel. */
@Suppress("unused")
val hideVisitorAdsPatch = bytecodePatch(
    name = "NoBrokerHood: Hide visitor-flow ads",
    description = "Skips visitor screen AdBannerSection composable (3.6.6 only).",
    default = true,
) {
    compatibleWith(NOBROKERHOOD_366)
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
        ).matchSingle().matchSingle().method.returnEarly()
    }
}

/** Home forum / feed ad card, not the home page itself. */
@Suppress("unused")
val hideHomeFeedAdsPatch = bytecodePatch(
    name = "NoBrokerHood: Hide feed ad cards",
    description = "Skips the home-feed advertisement composable (3.6.6 only).",
    default = true,
) {
    compatibleWith(NOBROKERHOOD_366)
    execute {
        Fingerprint(
            definingClass = "Lcom/app/nobrokerhood/features/home_feed/ui/HomeFeedAdBannerKt;",
            name = "HomeFeedAdBanner",
            returnType = "V",
            parameters = listOf(
                "Lcom/app/nobrokerhood/features/forum_new/home/model/FeedAdCard;",
                "Lkotlin/jvm/functions/Function3;", "Lkotlin/jvm/functions/Function3;",
                "Lc4/m;", "Li3/r;", "I", "I",
            ),
        ).matchSingle().matchSingle().method.returnEarly()
    }
}

/** App-level promotion display routine (not its visitor approval workflow). */
@Suppress("unused")
val hidePromotionDialogsPatch = bytecodePatch(
    name = "NoBrokerHood: Hide promotional popups",
    description = "Skips app-owned showAdvertisement method (3.6.6 only).",
    default = true,
) {
    compatibleWith(NOBROKERHOOD_366)
    execute {
        Fingerprint(
            definingClass = "Lcom/app/nobrokerhood/activities/lb;",
            name = "showAdvertisement",
            returnType = "V",
            parameters = listOf("Lcom/app/nobrokerhood/newnobrokerhood/promotions/data/PromotionsResponse;"),
        ).matchSingle().matchSingle().method.returnEarly()
    }
}

/**
 * Experimental. DoorPush.hasBanner checks whether the door push JSON's
 * bannerObject has a nonempty `url`. Return false so this banner is not
 * requested by callers. Core message processing is left intact.
 * Do not turn on before testing the real device notification path.
 */
@Suppress("unused")
val hideDoorNotificationBannersPatch = bytecodePatch(
    name = "NoBrokerHood: Hide door-notification banners (experimental)",
    description = "Reports no optional banner in door push data; preserves the push itself.",
    default = false,
) {
    compatibleWith(NOBROKERHOOD_366)
    execute {
        Fingerprint(
            definingClass = DOOR_PUSH,
            name = "hasBanner",
            returnType = "Z",
            parameters = emptyList(),
        ).matchSingle().method.returnEarly(false)
    }
}

/**
 * Experimental. This fragment may leave an empty container if its caller
 * assumes it always renders a banner. Never patch general approval classes.
 */
@Suppress("unused")
val hideEntryBannerFragmentPatch = bytecodePatch(
    name = "NoBrokerHood: Hide entry banner fragment (experimental)",
    description = "Skips display-entry promotional fragment setup; may leave empty space.",
    default = false,
) {
    compatibleWith(NOBROKERHOOD_366)
    execute {
        Fingerprint(
            definingClass = "Lcom/app/nobrokerhood/fragments/DisplayEntryBannerFragment;",
            name = "onViewCreated",
            returnType = "V",
            parameters = listOf("Landroid/view/View;", "Landroid/os/Bundle;"),
        ).matchSingle().matchSingle().method.returnEarly()
    }
}
