
package schwalbe.penilee.engine

import schwalbe.penilee.engine.*
import schwalbe.penilee.engine.input.VrController
import schwalbe.penilee.engine.gfx.*
import kotlin.math.abs
import kotlin.system.exitProcess
import org.lwjgl.openxr.*
import org.lwjgl.openxr.XR10.*
import org.lwjgl.openxr.KHROpenGLEnable.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.NULL
import org.lwjgl.opengl.GL33.*
import org.joml.*

fun initOpenXr(stack: MemoryStack): XrInstance? {
    val appInfo = XrApplicationInfo.calloc(stack).apply {
        applicationName(stack.UTF8("Penilee Island Railway"))
        applicationVersion(1)
        engineName(stack.UTF8("LWJGL"))
        engineVersion(1)
        apiVersion(XR_CURRENT_API_VERSION)
    }
    val extensions = stack.mallocPointer(1)
    extensions.put(stack.UTF8(
        KHROpenGLEnable.XR_KHR_OPENGL_ENABLE_EXTENSION_NAME
    ))
    extensions.flip()
    val createInfo = XrInstanceCreateInfo.calloc(stack).apply {
        type(XR_TYPE_INSTANCE_CREATE_INFO)
        next(NULL)
        applicationInfo(appInfo)
        enabledExtensionNames(extensions)
    }
    val instancePtr = stack.mallocPointer(1)
    val result = xrCreateInstance(createInfo, instancePtr)
    if(result != XR_SUCCESS) {
        println("No OpenXR runtime available")
        return null
    }
    return XrInstance(instancePtr.get(0), createInfo)
}

fun selectXrRuntime(stack: MemoryStack, instance: XrInstance): Long {
    val sysInfo = XrSystemGetInfo.calloc(stack).apply {
        type(XR_TYPE_SYSTEM_GET_INFO)
        formFactor(XR_FORM_FACTOR_HEAD_MOUNTED_DISPLAY)
    }
    val sysId = stack.mallocLong(1)
    val result = xrGetSystem(instance, sysInfo, sysId)
    if(result != XR_SUCCESS) {
        println("No VR system found")
        return -1
    }
    return sysId.get(0)
}

fun initXrSession(
    stack: MemoryStack, instance: XrInstance, sysId: Long
): XrSession {
    // determine graphics requirenments
    val graphicsRequirements = XrGraphicsRequirementsOpenGLKHR
        .calloc(stack)
        .apply {
            type(KHROpenGLEnable.XR_TYPE_GRAPHICS_REQUIREMENTS_OPENGL_KHR)
            next(NULL)
        }
    val requirementsResult = KHROpenGLEnable.xrGetOpenGLGraphicsRequirementsKHR(
        instance, sysId, graphicsRequirements
    )
    check(requirementsResult == XR_SUCCESS) {
        "Failed to get OpenGL graphics requirements: $requirementsResult" 
    }
    println("Min GL version: ${graphicsRequirements.minApiVersionSupported()}")
    println("Max GL version: ${graphicsRequirements.maxApiVersionSupported()}")
    // create graphics binding
    val graphicsBinding = XrGraphicsBindingOpenGLWin32KHR.calloc(stack).apply {
        type(XR_TYPE_GRAPHICS_BINDING_OPENGL_WIN32_KHR)
        hDC(org.lwjgl.opengl.WGL.wglGetCurrentDC())
        hGLRC(org.lwjgl.opengl.WGL.wglGetCurrentContext())
    }
    // create session
    val sessionCreateInfo = XrSessionCreateInfo.calloc(stack).apply {
        type(XR_TYPE_SESSION_CREATE_INFO)
        next(graphicsBinding.address())
        systemId(sysId)
    }
    val sessionPtr = stack.mallocPointer(1)
    val createResult = xrCreateSession(
        instance, sessionCreateInfo, sessionPtr
    )
    check(createResult == XR_SUCCESS) { 
        "Failed to create XR session: $createResult" 
    }
    val session = XrSession(sessionPtr.get(0), instance)
    // begin session
    val sessionBeginInfo = XrSessionBeginInfo.calloc(stack).apply {
        type(XR_TYPE_SESSION_BEGIN_INFO)
        primaryViewConfigurationType(
            XR_VIEW_CONFIGURATION_TYPE_PRIMARY_STEREO
        )
    }
    check(xrBeginSession(session, sessionBeginInfo) == XR_SUCCESS) {
        "Failed to begin session"
    }
    return session
}

fun getXrViewCount(
    stack: MemoryStack, instance: XrInstance, sysId: Long
): Int {
    val viewCountPtr = stack.mallocInt(1)
    val viewCountRes = xrEnumerateViewConfigurationViews(
        instance, sysId, XR_VIEW_CONFIGURATION_TYPE_PRIMARY_STEREO, 
        viewCountPtr, null
    )
    check(viewCountRes == XR_SUCCESS) { "Failed to determine view count" }
    return viewCountPtr.get(0)
}

fun getXrViewConfig(
    stack: MemoryStack, instance: XrInstance, sysId: Long, viewCount: Int
): XrViewConfigurationView {
    val views = XrViewConfigurationView.calloc(viewCount, stack)
    views.forEach { it.type(XR_TYPE_VIEW_CONFIGURATION_VIEW) }
    val viewConfRes = xrEnumerateViewConfigurationViews(
        instance, sysId, XR_VIEW_CONFIGURATION_TYPE_PRIMARY_STEREO, 
        stack.mallocInt(1), views
    )
    check(viewConfRes == XR_SUCCESS) { "Failed to get view configuration" }
    return views[0]
}

fun initXrSwapchain(
    stack: MemoryStack, instance: XrInstance, sysId: Long, session: XrSession,
    viewCount: Int, viewConfig: XrViewConfigurationView
): XrSwapchain {
    val viewWidth = viewConfig.recommendedImageRectWidth()
    val viewHeight = viewConfig.recommendedImageRectHeight()
    val viewSampleCount = viewConfig.recommendedSwapchainSampleCount()
    // create the actual swapchain
    val swapchainCreateInfo = XrSwapchainCreateInfo.calloc(stack).apply {
        type(XR_TYPE_SWAPCHAIN_CREATE_INFO)
        usageFlags(XR_SWAPCHAIN_USAGE_COLOR_ATTACHMENT_BIT.toLong())
        format(GL_RGBA8.toLong())
        sampleCount(viewSampleCount)
        width(viewWidth)
        height(viewHeight)
        faceCount(1)
        arraySize(viewCount)
        mipCount(1)
    }
    val swapchainPtr = stack.mallocPointer(1)
    val swapchainRes = xrCreateSwapchain(
        session, swapchainCreateInfo, swapchainPtr
    )
    check(swapchainRes == XR_SUCCESS) { "Failed to create swapchain" }
    return XrSwapchain(swapchainPtr.get(0), session)
}

fun getXrSwapchainImages(
    stack: MemoryStack, session: XrSession, 
    viewCount: Int, viewConfig: XrViewConfigurationView, swapchain: XrSwapchain
): Array<Texture3> {
    val viewWidth = viewConfig.recommendedImageRectWidth()
    val viewHeight = viewConfig.recommendedImageRectHeight()
    val countPtr = stack.mallocInt(1)
    val countRes = xrEnumerateSwapchainImages(swapchain, countPtr, null)
    check(countRes == XR_SUCCESS) { "Unable to get swapchain image count" }
    val count = countPtr.get(0)
    val images = XrSwapchainImageOpenGLKHR.calloc(count, stack)
    images.forEach { it.type(XR_TYPE_SWAPCHAIN_IMAGE_OPENGL_KHR) }
    val imageRes = xrEnumerateSwapchainImages(
        swapchain, countPtr, XrSwapchainImageBaseHeader.create(images)
    )
    check(imageRes == XR_SUCCESS) { "Unable to get swapchain images" }
    return Array(count) { 
        Texture3(images[it].image(), viewWidth, viewHeight, viewCount, false)
    }
}

fun initXrSpace(stack: MemoryStack, session: XrSession): XrSpace {
    val startingPose = XrPosef.calloc(stack)
        .orientation(XrQuaternionf.calloc(stack).set(0f, 0f, 0f, 1f))
        .`position$`(XrVector3f.calloc(stack).set(0f, 0f, 0f))
    val spaceCreateInfo = XrReferenceSpaceCreateInfo.calloc(stack)
        .type(XR_TYPE_REFERENCE_SPACE_CREATE_INFO)
        .referenceSpaceType(XR_REFERENCE_SPACE_TYPE_LOCAL)
        .poseInReferenceSpace(startingPose)
    val spacePtr = stack.mallocPointer(1)
    val spaceRes = xrCreateReferenceSpace(
        session, spaceCreateInfo, spacePtr
    )
    check(spaceRes == XR_SUCCESS) { "Unable to create reference space" }
    return XrSpace(spacePtr.get(0), session)
}

fun createXrActionSet(stack: MemoryStack, instance: XrInstance): XrActionSet {
    val actionSetInfo = XrActionSetCreateInfo.calloc(stack)
        .type(XR_TYPE_ACTION_SET_CREATE_INFO)
        .actionSetName(stack.UTF8("gameplay"))
        .localizedActionSetName(stack.UTF8("Gameplay"))
        .priority(0)
    val actionSetPtr = stack.mallocPointer(1)
    val actionSetRes = xrCreateActionSet(instance, actionSetInfo, actionSetPtr)
    check(actionSetRes == XR_SUCCESS) { "Failed to create action set" }
    return XrActionSet(actionSetPtr.get(0), instance)
}

class VrActionContext(
    val stack: MemoryStack, val instance: XrInstance,
    val session: XrSession, val space: XrSpace, val actionSet: XrActionSet
)

class VrActionProps(
    val name: String, val localName: String, val subactionPaths: List<String>
)

fun createXrPath(
    instance: XrInstance, path: String
): Long = MemoryStack.stackPush().use { s ->
    val pathPtr = s.mallocLong(1)
    val pathRes = xrStringToPath(instance, path, pathPtr)
    check(pathRes == XR_SUCCESS) { "Failed to create OpenXR path" }
    return pathPtr.get(0)
}

fun createXrAction(
    context: VrActionContext, type: Int, props: VrActionProps
): XrAction {
    val pathPtrs = context.stack.mallocLong(props.subactionPaths.size)
    props.subactionPaths.forEach { 
        pathPtrs.put(createXrPath(context.instance, it)) 
    }
    pathPtrs.flip()
    val actionInfo = XrActionCreateInfo.calloc(context.stack)
        .type(XR_TYPE_ACTION_CREATE_INFO)
        .actionType(type)
        .actionName(context.stack.UTF8(props.name))
        .localizedActionName(context.stack.UTF8(props.localName))
        .countSubactionPaths(props.subactionPaths.size)
        .subactionPaths(pathPtrs)
    val actionPtr = context.stack.mallocPointer(1)
    val actionRes = xrCreateAction(context.actionSet, actionInfo, actionPtr)
    check(actionRes == XR_SUCCESS) { "Failed to create OpenXR action" }
    return XrAction(actionPtr.get(0), context.actionSet)
}

fun createXrActionSpace(
    context: VrActionContext, props: VrActionProps, action: XrAction
): XrSpace {
    val startingPose = XrPosef.calloc(context.stack)
        .orientation(XrQuaternionf.calloc(context.stack).set(0f, 0f, 0f, 1f))
        .`position$`(XrVector3f.calloc(context.stack).set(0f, 0f, 0f))
    val spaceInfo = XrActionSpaceCreateInfo.calloc(context.stack)
        .type(XR_TYPE_ACTION_SPACE_CREATE_INFO)
        .action(action)
        .poseInActionSpace(startingPose)
        .subactionPath(createXrPath(context.instance, props.subactionPaths[0]))
    val spacePtr = context.stack.mallocPointer(1)
    val spaceRes = xrCreateActionSpace(context.session, spaceInfo, spacePtr)
    check(spaceRes == XR_SUCCESS) { "Failed to create OpenXR action space" }
    return XrSpace(spacePtr.get(0), context.session)
}

interface VrAction {
    val action: XrAction
    fun poll(predDispTime: Long)
}

class VrButtonAction(
    val ctx: VrActionContext, props: VrActionProps, 
    val button: VrController.Button
): VrAction {
    override val action: XrAction
        = createXrAction(ctx, XR_ACTION_TYPE_BOOLEAN_INPUT, props)
    override fun poll(predDispTime: Long) = MemoryStack.stackPush().use { s ->
        val state = XrActionStateBoolean.calloc(s)
            .type(XR_TYPE_ACTION_STATE_BOOLEAN)
        val getInfo = XrActionStateGetInfo.calloc(s)
            .type(XR_TYPE_ACTION_STATE_GET_INFO)
            .action(this.action)
        val getRes = xrGetActionStateBoolean(this.ctx.session, getInfo, state)
        if(getRes != XR_SUCCESS) { return }
        if(state.currentState()) { this.button.press() } 
        else { this.button.release() }
    }
}

class VrTriggerAction(
    val ctx: VrActionContext, props: VrActionProps,
    val withValue: (Float) -> Unit
): VrAction {
    override val action: XrAction
        = createXrAction(ctx, XR_ACTION_TYPE_FLOAT_INPUT, props)
    override fun poll(predDispTime: Long) = MemoryStack.stackPush().use { s ->
        val state = XrActionStateFloat.calloc(s)
            .type(XR_TYPE_ACTION_STATE_FLOAT)
        val getInfo = XrActionStateGetInfo.calloc(s)
            .type(XR_TYPE_ACTION_STATE_GET_INFO)
            .action(this.action)
        val getRes = xrGetActionStateFloat(this.ctx.session, getInfo, state)
        if(getRes != XR_SUCCESS) { return }
        withValue(state.currentState())
    }
}

class VrStickAction(
    val ctx: VrActionContext, props: VrActionProps,
    val withValue: (Vector2f) -> Unit
): VrAction {
    override val action: XrAction
        = createXrAction(ctx, XR_ACTION_TYPE_VECTOR2F_INPUT, props)
    override fun poll(predDispTime: Long) = MemoryStack.stackPush().use { s ->
        val state = XrActionStateVector2f.calloc(s)
            .type(XR_TYPE_ACTION_STATE_VECTOR2F)
        val getInfo = XrActionStateGetInfo.calloc(s)
            .type(XR_TYPE_ACTION_STATE_GET_INFO)
            .action(this.action)
        val getRes = xrGetActionStateVector2f(this.ctx.session, getInfo, state)
        if(getRes != XR_SUCCESS) { return }
        withValue(Vector2f(state.currentState().x(), state.currentState().y()))
    }
}

class VrPoseAction(
    val ctx: VrActionContext, props: VrActionProps,
    val withValue: (Vector3f, Vector3f, Vector3f) -> Unit
): VrAction {
    override val action: XrAction
        = createXrAction(ctx, XR_ACTION_TYPE_POSE_INPUT, props)
    val space = createXrActionSpace(ctx, props, this.action)
    override fun poll(predDispTime: Long) = MemoryStack.stackPush().use { s ->
        val l = XrSpaceLocation.calloc(s)
            .type(XR_TYPE_SPACE_LOCATION)
        val locateRes = xrLocateSpace(this.space, ctx.space, predDispTime, l)
        val wasSuccess: Boolean = locateRes == XR_SUCCESS 
            && (l.locationFlags() and XR_SPACE_LOCATION_POSITION_VALID_BIT.toLong()) != 0L
            && (l.locationFlags() and XR_SPACE_LOCATION_ORIENTATION_VALID_BIT.toLong()) != 0L
        if(!wasSuccess) { return }
        val rPos = l.pose().`position$`()
        val rQRot = l.pose().orientation()
        val qRot = Quaternionf(rQRot.x(), rQRot.y(), rQRot.z(), rQRot.w())
        withValue(
            Vector3f(rPos.x(), rPos.y(), rPos.z()),
            qRot.transform(Vector3f(NDC_INTO_SCREEN)),
            qRot.transform(Vector3f(UP))
        )
    }
}

class VrVibrateAction(
    val ctx: VrActionContext, props: VrActionProps
): VrAction {
    override val action: XrAction
        = createXrAction(ctx, XR_ACTION_TYPE_VIBRATION_OUTPUT, props)
    val vibration = XrHapticVibration.calloc(ctx.stack)
        .type(XR_TYPE_HAPTIC_VIBRATION)
        .frequency(XR_FREQUENCY_UNSPECIFIED)
    val actionInfo = XrHapticActionInfo.calloc(ctx.stack)
        .type(XR_TYPE_HAPTIC_ACTION_INFO)
        .action(this.action)
        .subactionPath(createXrPath(ctx.instance, props.subactionPaths[0]))
    override fun poll(predDispTime: Long) {}
    fun vibrate(amplitude: Float, durationNanos: Long) 
            = MemoryStack.stackPush().use { s ->        
        this.vibration
            .amplitude(amplitude)
            .duration(durationNanos)
        xrApplyHapticFeedback(
            this.ctx.session, this.actionInfo,
            XrHapticBaseHeader.create(this.vibration)
        )
    }
    fun stopVibration() = MemoryStack.stackPush().use { s ->
        xrStopHapticFeedback(this.ctx.session, this.actionInfo)
    }
}

fun suggestXrActionBindings(
    stack: MemoryStack, instance: XrInstance, profile: String, 
    sBindings: List<Pair<String, VrAction>>
) {
    val bindings = XrActionSuggestedBinding.calloc(sBindings.size, stack)
    sBindings.forEachIndexed { i, (bp, a) ->
        val binding = bindings.get(i)
        binding.action(a.action)
        binding.binding(createXrPath(instance, bp))
    }
    val suggestedBinding = XrInteractionProfileSuggestedBinding.calloc(stack)
        .type(XR_TYPE_INTERACTION_PROFILE_SUGGESTED_BINDING)
        .interactionProfile(createXrPath(instance, profile))
        .suggestedBindings(bindings)
    val suggestRes = xrSuggestInteractionProfileBindings(
        instance, suggestedBinding
    )
    check(suggestRes == XR_SUCCESS) { "Failed to suggest input bindings" }
}

fun populateXrActionSet(
    stack: MemoryStack, instance: XrInstance, session: XrSession, 
    space: XrSpace, actionSet: XrActionSet
): List<VrAction> {
    val ctx = VrActionContext(stack, instance, session, space, actionSet)
    val buttonsSaP: List<String> = listOf("/user/hand/left", "/user/hand/right")
    val aButton = VrButtonAction(ctx, VrActionProps(
        "a_button_action", "A Button", buttonsSaP
    ), VrController.Button.A)
    val bButton = VrButtonAction(ctx, VrActionProps(
        "b_button_action", "B Button", buttonsSaP
    ), VrController.Button.B)
    val xButton = VrButtonAction(ctx, VrActionProps(
        "x_button_action", "X Button", buttonsSaP
    ), VrController.Button.X)
    val yButton = VrButtonAction(ctx, VrActionProps(
        "y_button_action", "Y Button", buttonsSaP
    ), VrController.Button.Y)
    val menuButton = VrButtonAction(ctx, VrActionProps(
        "menu_button_action", "Menu Button", buttonsSaP
    ), VrController.Button.MENU)
    val leftTrigger = VrTriggerAction(ctx, VrActionProps(
        "left_trigger_action", "Left Trigger", listOf("/user/hand/left")
    )) { value -> VrController.LEFT.trigger = value }
    val rightTrigger = VrTriggerAction(ctx, VrActionProps(
        "right_trigger_action", "Right Trigger", listOf("/user/hand/right")
    )) { value -> VrController.RIGHT.trigger = value }
    val leftSqueeze = VrTriggerAction(ctx, VrActionProps(
        "left_squeeze_action", "Left Squeeze", listOf("/user/hand/left")
    )) { value -> VrController.LEFT.squeeze = value }
    val rightSqueeze = VrTriggerAction(ctx, VrActionProps(
        "right_squeeze_action", "Right Squeeze", listOf("/user/hand/right")
    )) { value -> VrController.RIGHT.squeeze = value }
    val leftStick = VrStickAction(ctx, VrActionProps(
        "left_stick_action", "Left Stick", listOf("/user/hand/left")
    )) { value -> VrController.LEFT.stick = value }
    val rightStick = VrStickAction(ctx, VrActionProps(
        "right_stick_action", "Right Stick", listOf("/user/hand/right")
    )) { value -> VrController.RIGHT.stick = value }
    val leftStickButton = VrButtonAction(ctx, VrActionProps(
        "left_stick_click_action", "Left Stick Click", listOf("/user/hand/left")
    ), VrController.Button.STICK_L)
    val rightStickButton = VrButtonAction(ctx, VrActionProps(
        "right_stick_click_action", "Right Stick Click", 
        listOf("/user/hand/right")
    ), VrController.Button.STICK_R)
    val leftGrip = VrPoseAction(ctx, VrActionProps(
        "left_grip_pose_action", "Left Grip Pose", listOf("/user/hand/left")
    )) { pos, dir, up ->
        VrController.LEFT.gripPos = pos
        VrController.LEFT.gripDir = dir
        VrController.LEFT.gripUp = up
    }
    val rightGrip = VrPoseAction(ctx, VrActionProps(
        "right_grip_pose_action", "Right Grip Pose", listOf("/user/hand/right")
    )) { pos, dir, up ->
        VrController.RIGHT.gripPos = pos
        VrController.RIGHT.gripDir = dir
        VrController.RIGHT.gripUp = up
    }
    val leftAim = VrPoseAction(ctx, VrActionProps(
        "left_aim_pose_action", "Left Aim Pose", listOf("/user/hand/left")
    )) { pos, dir, up ->
        VrController.LEFT.aimPos = pos
        VrController.LEFT.aimDir = dir
        VrController.LEFT.aimUp = up
    }
    val rightAim = VrPoseAction(ctx, VrActionProps(
        "right_aim_pose_action", "Right Aim Pose", listOf("/user/hand/right")
    )) { pos, dir, up ->
        VrController.RIGHT.aimPos = pos
        VrController.RIGHT.aimDir = dir
        VrController.RIGHT.aimUp = up
    }
    val leftVibrate = VrVibrateAction(ctx, VrActionProps(
        "left_vibrate_action", "Left Vibration", listOf("/user/hand/left")
    ))
    VrController.LEFT.vibrateImpl = leftVibrate::vibrate
    VrController.LEFT.stopVibrationImpl = leftVibrate::stopVibration
    val rightVibrate = VrVibrateAction(ctx, VrActionProps(
        "right_vibrate_action", "Right Vibration", listOf("/user/hand/right")
    ))
    VrController.RIGHT.vibrateImpl = rightVibrate::vibrate
    VrController.RIGHT.stopVibrationImpl = rightVibrate::stopVibration
    suggestXrActionBindings(
        stack, instance, "/interaction_profiles/oculus/touch_controller", 
        listOf(
            "/user/hand/right/input/a/click" to aButton,
            "/user/hand/right/input/b/click" to bButton,
            "/user/hand/left/input/x/click" to xButton,
            "/user/hand/left/input/y/click" to yButton,
            "/user/hand/left/input/menu/click" to menuButton,
            "/user/hand/left/input/trigger/value" to leftTrigger,
            "/user/hand/right/input/trigger/value" to rightTrigger,
            "/user/hand/left/input/squeeze/value" to leftSqueeze,
            "/user/hand/right/input/squeeze/value" to rightSqueeze,
            "/user/hand/left/input/thumbstick" to leftStick,
            "/user/hand/right/input/thumbstick" to rightStick,
            "/user/hand/left/input/thumbstick/click" to leftStickButton,
            "/user/hand/right/input/thumbstick/click" to rightStickButton,
            "/user/hand/left/input/grip/pose" to leftGrip,
            "/user/hand/right/input/grip/pose" to rightGrip,
            "/user/hand/left/input/aim/pose" to leftAim,
            "/user/hand/right/input/aim/pose" to rightAim,
            "/user/hand/left/output/haptic" to leftVibrate,
            "/user/hand/right/output/haptic" to rightVibrate
        )
    )
    suggestXrActionBindings(
        stack, instance, "/interaction_profiles/valve/index_controller", 
        listOf(
            "/user/hand/right/input/a/click" to aButton,
            "/user/hand/right/input/b/click" to bButton,
            "/user/hand/left/input/a/click" to xButton,
            "/user/hand/left/input/b/click" to yButton,
            "/user/hand/left/input/system/click" to menuButton,
            "/user/hand/left/input/trigger/value" to leftTrigger,
            "/user/hand/right/input/trigger/value" to rightTrigger,
            "/user/hand/left/input/squeeze/value" to leftSqueeze,
            "/user/hand/right/input/squeeze/value" to rightSqueeze,
            "/user/hand/left/input/thumbstick" to leftStick,
            "/user/hand/right/input/thumbstick" to rightStick,
            "/user/hand/left/input/thumbstick/click" to leftStickButton,
            "/user/hand/right/input/thumbstick/click" to rightStickButton,
            "/user/hand/left/input/grip/pose" to leftGrip,
            "/user/hand/right/input/grip/pose" to rightGrip,
            "/user/hand/left/input/aim/pose" to leftAim,
            "/user/hand/right/input/aim/pose" to rightAim,
            "/user/hand/left/output/haptic" to leftVibrate,
            "/user/hand/right/output/haptic" to rightVibrate
        )
    )
    suggestXrActionBindings(
        stack, instance, "/interaction_profiles/microsoft/motion_controller", 
        listOf(
            "/user/hand/left/input/menu/click" to menuButton,
            "/user/hand/left/input/trigger/value" to leftTrigger,
            "/user/hand/right/input/trigger/value" to rightTrigger,
            "/user/hand/left/input/squeeze/click" to leftSqueeze,
            "/user/hand/right/input/squeeze/click" to rightSqueeze,
            "/user/hand/left/input/thumbstick" to leftStick,
            "/user/hand/right/input/thumbstick" to rightStick,
            "/user/hand/left/input/thumbstick/click" to leftStickButton,
            "/user/hand/right/input/thumbstick/click" to rightStickButton,
            "/user/hand/left/input/grip/pose" to leftGrip,
            "/user/hand/right/input/grip/pose" to rightGrip,
            "/user/hand/left/input/aim/pose" to leftAim,
            "/user/hand/right/input/aim/pose" to rightAim,
            "/user/hand/left/output/haptic" to leftVibrate,
            "/user/hand/right/output/haptic" to rightVibrate
        )
    )
    suggestXrActionBindings(
        stack, instance, "/interaction_profiles/khr/simple_controller", 
        listOf(
            "/user/hand/left/input/menu/click" to menuButton,
            "/user/hand/left/input/select/click" to leftTrigger,
            "/user/hand/right/input/select/click" to rightTrigger,
            "/user/hand/left/input/aim/pose" to leftAim,
            "/user/hand/right/input/aim/pose" to rightAim,
            "/user/hand/right/output/haptic" to leftVibrate,
            "/user/hand/right/output/haptic" to rightVibrate
        )
    )
    suggestXrActionBindings(
        stack, instance, "/interaction_profiles/htc/vive_controller", 
        listOf(
            "/user/hand/left/input/menu/click" to menuButton,
            "/user/hand/left/input/trigger/value" to leftTrigger,
            "/user/hand/right/input/trigger/value" to rightTrigger,
            "/user/hand/left/input/squeeze/click" to leftSqueeze,
            "/user/hand/right/input/squeeze/click" to rightSqueeze,
            "/user/hand/left/input/trackpad" to leftStick,
            "/user/hand/right/input/trackpad" to rightStick,
            "/user/hand/left/input/trackpad/click" to leftStickButton,
            "/user/hand/right/input/trackpad/click" to rightStickButton,
            "/user/hand/left/input/grip/pose" to leftGrip,
            "/user/hand/right/input/grip/pose" to rightGrip,
            "/user/hand/left/input/aim/pose" to leftAim,
            "/user/hand/right/input/aim/pose" to rightAim,
            "/user/hand/left/output/haptic" to leftVibrate,
            "/user/hand/right/output/haptic" to rightVibrate
        )
    )
    return listOf(
        aButton, bButton, xButton, yButton, menuButton,
        leftTrigger, rightTrigger, leftSqueeze, rightSqueeze,
        leftStick, rightStick, leftStickButton, rightStickButton,
        leftGrip, rightGrip, leftAim, rightAim
    )
}

fun attachXrActionSet(
    session: XrSession, actionSet: XrActionSet
) = MemoryStack.stackPush().use { stack ->
    val actionSetPtr = stack.mallocPointer(1)
    actionSetPtr.put(actionSet.address())
    actionSetPtr.flip()
    val attachInfo = XrSessionActionSetsAttachInfo.calloc(stack)
        .type(XR_TYPE_SESSION_ACTION_SETS_ATTACH_INFO)
        .actionSets(actionSetPtr)
    val attachRes = xrAttachSessionActionSets(session, attachInfo)
    check(attachRes == XR_SUCCESS) { "Failed to attach action set to session: ${attachRes}" }
}

class VrContext(
    val instance: XrInstance,
    val sysId: Long,
    val session: XrSession,
    val viewCount: Int,
    val swapchain: XrSwapchain,
    val images: Array<Texture3>,
    val imageFBOs: Array<Array<Framebuffer>>,
    val space: XrSpace,
    val cameras: Array<Camera>,
    val actionSet: XrActionSet,
    val actions: List<VrAction>
) {
    var actionSetAttached: Boolean = false
    var isFocused: Boolean = false
    
    fun beginFrame(stack: MemoryStack): Long {        
        // wait for the next frame
        val frameWaitInfo = XrFrameWaitInfo.calloc(stack)
            .type(XR_TYPE_FRAME_WAIT_INFO)
        val frameState = XrFrameState.calloc(stack)
            .type(XR_TYPE_FRAME_STATE)
        val waitRes = xrWaitFrame(
            this.session, frameWaitInfo, frameState
        )
        check(waitRes == XR_SUCCESS) { "Failed to await frame" }
        // begin the next frame
        val frameBeginInfo = XrFrameBeginInfo.calloc(stack)
            .type(XR_TYPE_FRAME_BEGIN_INFO)
        val beginRes = xrBeginFrame(this.session, frameBeginInfo)
        check(beginRes == XR_SUCCESS) { "Failed to begin frame" }
        return frameState.predictedDisplayTime()
    }

    fun pollEvents(stack: MemoryStack) {
        val event = XrEventDataBuffer.calloc(stack)
            .type(XR_TYPE_EVENT_DATA_BUFFER)
        while(xrPollEvent(this.instance, event) == XR_SUCCESS) {
            when(event.type()) {
                XR_TYPE_EVENT_DATA_SESSION_STATE_CHANGED -> {
                    val changed = XrEventDataSessionStateChanged
                        .create(event.address())
                    val newState = changed.state()
                    this.isFocused = newState == XR_SESSION_STATE_FOCUSED
                    if(this.isFocused && !this.actionSetAttached) {
                        attachXrActionSet(session, actionSet)
                        this.actionSetAttached = true
                    }
                    val sessionEnded = newState == XR_SESSION_STATE_EXITING
                        || newState == XR_SESSION_STATE_STOPPING
                        || newState == XR_SESSION_STATE_LOSS_PENDING
                    if(sessionEnded) {
                        this.destroy()
                        exitProcess(0)
                    }
                }
            }
            event.type(XR_TYPE_EVENT_DATA_BUFFER)
        }
    }

    fun pollActions(stack: MemoryStack, predDispTime: Long) {
        if(!this.isFocused) { return }
        val activeActionSets = XrActiveActionSet.calloc(1, stack)
        activeActionSets.forEach { it
            .actionSet(this.actionSet)
            .subactionPath(0L)
        }
        val syncInfo = XrActionsSyncInfo.calloc(stack)
            .type(XR_TYPE_ACTIONS_SYNC_INFO)
            .countActiveActionSets(1)
            .activeActionSets(activeActionSets)
        val syncRes = xrSyncActions(this.session, syncInfo)
        check(syncRes == XR_SUCCESS) { "Failed to sync OpenXR actions: ${syncRes}" }
        this.actions.forEach { it.poll(predDispTime) }
    }

    fun locateViews(stack: MemoryStack, predDispTime: Long): XrView.Buffer {
        val locateInfo = XrViewLocateInfo.calloc(stack)
            .type(XR_TYPE_VIEW_LOCATE_INFO)
            .viewConfigurationType(XR_VIEW_CONFIGURATION_TYPE_PRIMARY_STEREO)
            .displayTime(predDispTime)
            .space(this.space)
        val viewState = XrViewState.calloc(stack)
            .type(XR_TYPE_VIEW_STATE)
        val views = XrView.calloc(this.viewCount, stack)
        views.forEach { it.type(XR_TYPE_VIEW) }
        val locateRes = xrLocateViews(
            this.session, locateInfo, viewState, stack.mallocInt(1), views
        )
        check(locateRes == XR_SUCCESS) { "Failed to locate views" }
        return views
    }

    fun updateCameras(views: XrView.Buffer) {
        for(eye in 0..<this.viewCount) {
            val cam = this.cameras[eye]
            val pose: XrPosef = views[eye].pose()
            cam.pos.set(
                pose.`position$`().x(),
                pose.`position$`().y(),
                pose.`position$`().z()
            )
            val orientation = Quaternionf(
                pose.orientation().x(),
                pose.orientation().y(),
                pose.orientation().z(),
                pose.orientation().w()
            )
            orientation.transform(NDC_INTO_SCREEN, cam.dir)
            orientation.transform(UP, cam.up)
            val fov: XrFovf = views[eye].fov()
            cam.fovLeft = fov.angleLeft()
            cam.fovRight = fov.angleRight()
            cam.fovDown = fov.angleDown()
            cam.fovUp = fov.angleUp()
        }
    }

    fun computeCameraAverage(dest: Camera): Float {
        dest.pos.set(0f, 0f, 0f)
        dest.dir.set(0f, 0f, 0f)
        dest.up.set(0f, 0f, 0f)
        var hFov: Float = 0f
        for(cam in this.cameras) {
            dest.pos.add(cam.pos)
            dest.dir.add(cam.dir)
            dest.up.add(cam.up)
            hFov += abs(cam.fovLeft) + abs(cam.fovRight)
        }
        val camN: Float = this.cameras.size.toFloat()
        dest.pos.div(camN)
        dest.dir.div(camN)
        dest.up.div(camN)
        hFov /= camN
        return hFov
    }

    fun withSwapchain(stack: MemoryStack, f: (Int) -> Unit) {
        // acquire swapchain
        val acquireInfo = XrSwapchainImageAcquireInfo.calloc(stack)
            .type(XR_TYPE_SWAPCHAIN_IMAGE_ACQUIRE_INFO)
        val imgIndexPtr = stack.mallocInt(1)
        val acquireRes = xrAcquireSwapchainImage(
            this.swapchain, acquireInfo, imgIndexPtr
        )
        check(acquireRes == XR_SUCCESS) { "Failed to acquire swapchain" }
        // wait for swapchain image
        val waitInfo = XrSwapchainImageWaitInfo.calloc(stack)
            .type(XR_TYPE_SWAPCHAIN_IMAGE_WAIT_INFO)
            .timeout(Long.MAX_VALUE)
        val waitRes = xrWaitSwapchainImage(this.swapchain, waitInfo)
        check(waitRes == XR_SUCCESS) { "Failed to wait for swapchain" }
        // call handler
        f(imgIndexPtr.get(0))
        // release swapchain
        val releaseInfo = XrSwapchainImageReleaseInfo.calloc(stack)
            .type(XR_TYPE_SWAPCHAIN_IMAGE_RELEASE_INFO)
        val releaseRes = xrReleaseSwapchainImage(
            this.swapchain, releaseInfo
        )
        check(releaseRes == XR_SUCCESS) { "Failed to release swapchain" }
    }

    fun submitFrame(
        stack: MemoryStack, predDispTime: Long, views: XrView.Buffer
    ) {
        val projectionViews = XrCompositionLayerProjectionView
            .calloc(this.viewCount, stack)
        for(eye in 0..<this.viewCount) {
            val imageRect = XrRect2Di.calloc(stack)
                .offset(XrOffset2Di.calloc(stack).set(0,0))
                .extent(XrExtent2Di.calloc(stack).set(
                    this.images[0].width, this.images[0].height
                ))
            val subImage = XrSwapchainSubImage.calloc(stack)
                .swapchain(this.swapchain)
                .imageRect(imageRect)
                .imageArrayIndex(eye)
            projectionViews[eye]
                .type(XR_TYPE_COMPOSITION_LAYER_PROJECTION_VIEW)
                .pose(views[eye].pose())
                .fov(views[eye].fov())
                .subImage(subImage)
        }
        val projectionLayer = XrCompositionLayerProjection.calloc(stack)
            .type(XR_TYPE_COMPOSITION_LAYER_PROJECTION)
            .space(this.space)
            .views(projectionViews)
        val frameEndInfo = XrFrameEndInfo.calloc(stack)
            .type(XR_TYPE_FRAME_END_INFO)
            .displayTime(predDispTime)
            .environmentBlendMode(XR_ENVIRONMENT_BLEND_MODE_OPAQUE)
            .layerCount(1)
            .layers(stack.pointers(projectionLayer.address()))
        val submitRes = xrEndFrame(this.session, frameEndInfo)
        check(submitRes == XR_SUCCESS) { "Failed to submit frame" }
    }

    fun requestSessionEnd() {
        check(xrRequestExitSession(this.session) == XR_SUCCESS) {
            "Failed to request OpenXR session end"
        }
    }

    fun destroy() {
        check(xrEndSession(this.session) == XR_SUCCESS) {
            "Failed to end OpenXR session" 
        }
        check(xrDestroySwapchain(this.swapchain) == XR_SUCCESS) {
            "Failed to destroy OpenXR swapchain"
        }
        check(xrDestroySpace(this.space) == XR_SUCCESS) {
            "Failed to destroy OpenXR space"
        }
        check(xrDestroySession(this.session) == XR_SUCCESS) {
            "Failed to destroy OpenXR session"
        }
        check(xrDestroyInstance(this.instance) == XR_SUCCESS) {
            "Failed to destroy OpenXR instance"
        }
    }

    fun runLoop(
        window: Window,
        update: (Float, Camera) -> Unit, 
        render: (Camera, Framebuffer, Float) -> Unit
    ) {
        val windowDest: Framebuffer = window.framebuffer()
        val windowCamera = Camera()
        val deltaTimeState = DeltaTimeState()
        while(true) {
            if(window.shouldClose()) { this.requestSessionEnd() }
            window.pollEvents()
            val deltaTime: Float = deltaTimeState.computeDeltaTime()
            var hFov: Float = 0f
            MemoryStack.stackPush().use { stack ->
                val predDispTime: Long = this.beginFrame(stack)
                this.pollEvents(stack)
                this.locateViews(stack, predDispTime)
                this.pollActions(stack, predDispTime)
                val views: XrView.Buffer = this.locateViews(stack, predDispTime)
                this.updateCameras(views)
                hFov = this.computeCameraAverage(windowCamera)
                windowCamera.setHorizontalFov(
                    hFov, window.width.toFloat() / window.height.toFloat()
                )
                update(deltaTime, windowCamera)
                this.withSwapchain(stack) { imgIndex ->
                    for(eye in 0..<this.viewCount) {
                        val camera = this.cameras[eye]
                        val framebuffer = this.imageFBOs[imgIndex][eye]
                        render(camera, framebuffer, deltaTime)
                    }
                }
                this.submitFrame(stack, predDispTime, views)
            }
            render(windowCamera, windowDest, deltaTime)
            window.swapBuffers()
        }
    }

}

fun withVrContext(f: (VrContext) -> Unit): Boolean {
    MemoryStack.stackPush().use { stack ->
        val instance: XrInstance? = initOpenXr(stack)
        if(instance == null) { return false }
        println("Created OpenXR instance")
        val sysId: Long = selectXrRuntime(stack, instance)
        if(sysId == -1L) { return false }
        println("Selected OpenXR runtime with ID ${sysId}")
        val session: XrSession = initXrSession(stack, instance, sysId)
        println("Created OpenXR session")
        val viewCount: Int = getXrViewCount(stack, instance, sysId)
        val viewConfig: XrViewConfigurationView = getXrViewConfig(
            stack, instance, sysId, viewCount
        )
        val swapchain: XrSwapchain = initXrSwapchain(
            stack, instance, sysId, session, viewCount, viewConfig
        )
        val images: Array<Texture3> = getXrSwapchainImages(
            stack, session, viewCount, viewConfig, swapchain
        )
        val imageFBOs: Array<Array<Framebuffer>> = Array(images.size) { imgI ->
            Array(viewCount) { eyeI ->
                val viewTex: Texture3 = images[imgI]
                val depthTex: Texture2 = Texture2(
                    viewTex.width, viewTex.height, 
                    TextureFormat.DEPTH24_STENCIL8
                )
                Framebuffer().attachColor(viewTex, eyeI).attachDepth(depthTex)
            }
        }
        println("Created OpenXR swapchain")
        val space: XrSpace = initXrSpace(stack, session)
        val cameras: Array<Camera> = Array(viewCount) { Camera() }
        println("Created reference space")
        val actionSet: XrActionSet = createXrActionSet(stack, instance)
        val actions: List<VrAction> = populateXrActionSet(
            stack, instance, session, space, actionSet
        )
        println("Created actions")
        val vr = VrContext(
            instance, sysId, session, viewCount, 
            swapchain, images, imageFBOs, space, cameras,
            actionSet, actions
        )
        f(vr)
        vr.destroy()
    }
    return true
}