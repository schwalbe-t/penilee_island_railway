
package schwalbe.penilee.engine

import schwalbe.penilee.engine.gfx.*
import org.lwjgl.openxr.*
import org.lwjgl.openxr.KHROpenGLEnable.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.NULL
import org.lwjgl.opengl.GL11
import org.joml.*

fun initOpenXr(stack: MemoryStack): XrInstance {
    val appInfo = XrApplicationInfo.calloc(stack).apply {
        applicationName(stack.UTF8("Penilee Island Railway"))
        applicationVersion(1)
        engineName(stack.UTF8("LWJGL"))
        engineVersion(1)
        apiVersion(XR10.XR_CURRENT_API_VERSION)
    }
    val extensions = stack.mallocPointer(1)
    extensions.put(stack.UTF8(
        KHROpenGLEnable.XR_KHR_OPENGL_ENABLE_EXTENSION_NAME
    ))
    extensions.flip()
    val createInfo = XrInstanceCreateInfo.calloc(stack).apply {
        type(XR10.XR_TYPE_INSTANCE_CREATE_INFO)
        next(NULL)
        applicationInfo(appInfo)
        enabledExtensionNames(extensions)
    }
    val instancePtr = stack.mallocPointer(1)
    val result = XR10.xrCreateInstance(createInfo, instancePtr)
    check(result == XR10.XR_SUCCESS) { "Failed to create XR instance: $result" }
    return XrInstance(instancePtr.get(0), createInfo)
}

fun selectXrRuntime(stack: MemoryStack, instance: XrInstance): Long {
    val sysInfo = XrSystemGetInfo.calloc(stack).apply {
        type(XR10.XR_TYPE_SYSTEM_GET_INFO)
        formFactor(XR10.XR_FORM_FACTOR_HEAD_MOUNTED_DISPLAY)
    }
    val sysId = stack.mallocLong(1)
    val result = XR10.xrGetSystem(instance, sysInfo, sysId)
    if(result != XR10.XR_SUCCESS) {
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
    check(requirementsResult == XR10.XR_SUCCESS) {
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
        type(XR10.XR_TYPE_SESSION_CREATE_INFO)
        next(graphicsBinding.address())
        systemId(sysId)
    }
    val sessionPtr = stack.mallocPointer(1)
    val createResult = XR10.xrCreateSession(
        instance, sessionCreateInfo, sessionPtr
    )
    check(createResult == XR10.XR_SUCCESS) { 
        "Failed to create XR session: $createResult" 
    }
    val session = XrSession(sessionPtr.get(0), instance)
    // begin session
    val sessionBeginInfo = XrSessionBeginInfo.calloc(stack).apply {
        type(XR10.XR_TYPE_SESSION_BEGIN_INFO)
        primaryViewConfigurationType(
            XR10.XR_VIEW_CONFIGURATION_TYPE_PRIMARY_STEREO
        )
    }
    check(XR10.xrBeginSession(session, sessionBeginInfo) == XR10.XR_SUCCESS) {
        "Failed to begin session"
    }
    return session
}

fun getXrViewCount(
    stack: MemoryStack, instance: XrInstance, sysId: Long
): Int {
    val viewCountPtr = stack.mallocInt(1)
    val viewCountRes = XR10.xrEnumerateViewConfigurationViews(
        instance, sysId, XR10.XR_VIEW_CONFIGURATION_TYPE_PRIMARY_STEREO, 
        viewCountPtr, null
    )
    check(viewCountRes == XR10.XR_SUCCESS) { "Failed to determine view count" }
    return viewCountPtr.get(0)
}

fun getXrViewConfig(
    stack: MemoryStack, instance: XrInstance, sysId: Long, viewCount: Int
): XrViewConfigurationView {
    val views = XrViewConfigurationView.calloc(viewCount, stack)
    views.forEach { it.type(XR10.XR_TYPE_VIEW_CONFIGURATION_VIEW) }
    val viewConfRes = XR10.xrEnumerateViewConfigurationViews(
        instance, sysId, XR10.XR_VIEW_CONFIGURATION_TYPE_PRIMARY_STEREO, 
        stack.mallocInt(1), views
    )
    check(viewConfRes == XR10.XR_SUCCESS) { "Failed to get view configuration" }
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
        type(XR10.XR_TYPE_SWAPCHAIN_CREATE_INFO)
        usageFlags(XR10.XR_SWAPCHAIN_USAGE_COLOR_ATTACHMENT_BIT.toLong())
        format(GL11.GL_RGBA8.toLong())
        sampleCount(viewSampleCount)
        width(viewWidth)
        height(viewHeight)
        faceCount(1)
        arraySize(viewCount)
        mipCount(1)
    }
    val swapchainPtr = stack.mallocPointer(1)
    val swapchainRes = XR10.xrCreateSwapchain(
        session, swapchainCreateInfo, swapchainPtr
    )
    check(swapchainRes == XR10.XR_SUCCESS) { "Failed to create swapchain" }
    return XrSwapchain(swapchainPtr.get(0), session)
}

fun getXrSwapchainImages(
    stack: MemoryStack, session: XrSession, 
    viewCount: Int, viewConfig: XrViewConfigurationView, swapchain: XrSwapchain
): Array<Texture> {
    val viewWidth = viewConfig.recommendedImageRectWidth()
    val viewHeight = viewConfig.recommendedImageRectHeight()
    val countPtr = stack.mallocInt(1)
    val countRes = XR10.xrEnumerateSwapchainImages(swapchain, countPtr, null)
    check(countRes == XR10.XR_SUCCESS) { "Unable to get swapchain image count" }
    val count = countPtr.get(0)
    val images = XrSwapchainImageOpenGLKHR.calloc(count, stack)
    images.forEach { it.type(XR_TYPE_SWAPCHAIN_IMAGE_OPENGL_KHR) }
    val imageRes = XR10.xrEnumerateSwapchainImages(
        swapchain, countPtr, XrSwapchainImageBaseHeader.create(images)
    )
    check(imageRes == XR10.XR_SUCCESS) { "Unable to get swapchain images" }
    return Array(count) { 
        Texture(images[it].image(), viewWidth, viewHeight, viewCount, false)
    }
}

fun initXrSpace(stack: MemoryStack, session: XrSession): XrSpace {
    val startingPose = XrPosef.calloc(stack)
        .orientation(XrQuaternionf.calloc(stack).set(0f, 0f, 0f, 1f))
        .`position$`(XrVector3f.calloc(stack).set(0f, 0f, 0f))
    val spaceCreateInfo = XrReferenceSpaceCreateInfo.calloc(stack)
        .type(XR10.XR_TYPE_REFERENCE_SPACE_CREATE_INFO)
        .referenceSpaceType(XR10.XR_REFERENCE_SPACE_TYPE_LOCAL)
        .poseInReferenceSpace(startingPose)
    val spacePtr = stack.mallocPointer(1)
    val spaceRes = XR10.xrCreateReferenceSpace(
        session, spaceCreateInfo, spacePtr
    )
    check(spaceRes == XR10.XR_SUCCESS) { "Unable to create reference space" }
    return XrSpace(spacePtr.get(0), session)
}

class VrContext(
    val instance: XrInstance,
    val sysId: Long,
    val session: XrSession,
    val viewCount: Int,
    val swapchain: XrSwapchain,
    val images: Array<Texture>,
    val imageFBOs: Array<Array<Framebuffer>>,
    val space: XrSpace,
    val cameras: Array<Camera>
) {

    fun beginFrame(stack: MemoryStack): Long {        
        // wait for the next frame
        val frameWaitInfo = XrFrameWaitInfo.calloc(stack)
            .type(XR10.XR_TYPE_FRAME_WAIT_INFO)
        val frameState = XrFrameState.calloc(stack)
            .type(XR10.XR_TYPE_FRAME_STATE)
        val waitRes = XR10.xrWaitFrame(
            this.session, frameWaitInfo, frameState
        )
        check(waitRes == XR10.XR_SUCCESS) { "Failed to await frame" }
        // begin the next frame
        val frameBeginInfo = XrFrameBeginInfo.calloc(stack)
            .type(XR10.XR_TYPE_FRAME_BEGIN_INFO)
        val beginRes = XR10.xrBeginFrame(this.session, frameBeginInfo)
        check(beginRes == XR10.XR_SUCCESS) { "Failed to begin frame" }
        return frameState.predictedDisplayTime()
    }

    fun pollEvents(stack: MemoryStack) {
        val event = XrEventDataBuffer.calloc(stack)
            .type(XR10.XR_TYPE_EVENT_DATA_BUFFER)
        while(XR10.xrPollEvent(this.instance, event) == XR10.XR_SUCCESS) {
            when(event.type()) {
                XR10.XR_TYPE_EVENT_DATA_SESSION_STATE_CHANGED -> {
                    val changed = XrEventDataSessionStateChanged
                        .create(event.address())
                    val newState = changed.state()
                    val sessionEnded = newState == XR10.XR_SESSION_STATE_EXITING 
                        || newState == XR10.XR_SESSION_STATE_LOSS_PENDING
                    check(!sessionEnded) { "Session ended" }
                }
            }
            event.type(XR10.XR_TYPE_EVENT_DATA_BUFFER)
        }
    }

    fun locateViews(stack: MemoryStack, predDispTime: Long): XrView.Buffer {
        val locateInfo = XrViewLocateInfo.calloc(stack)
            .type(XR10.XR_TYPE_VIEW_LOCATE_INFO)
            .viewConfigurationType(XR10.XR_VIEW_CONFIGURATION_TYPE_PRIMARY_STEREO)
            .displayTime(predDispTime)
            .space(this.space)
        val viewState = XrViewState.calloc(stack)
            .type(XR10.XR_TYPE_VIEW_STATE)
        val views = XrView.calloc(this.viewCount, stack)
        views.forEach { it.type(XR10.XR_TYPE_VIEW) }
        val locateRes = XR10.xrLocateViews(
            this.session, locateInfo, viewState, stack.mallocInt(1), views
        )
        check(locateRes == XR10.XR_SUCCESS) { "Failed to locate views" }
        return views
    }

    fun updateCameras(views: XrView.Buffer) {
        for(eye in 0..<this.viewCount) {
            val cam = this.cameras[eye]
            val pose: XrPosef = views[eye].pose()
            cam.pos = Vector3f(
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
            cam.dir = orientation.transform(Vector3f(0f, 0f, -1f))
            cam.up = orientation.transform(Vector3f(0f, 1f, 0f))
            val fov: XrFovf = views[eye].fov()
            cam.fovLeft = fov.angleLeft()
            cam.fovRight = fov.angleRight()
            cam.fovDown = fov.angleDown()
            cam.fovUp = fov.angleUp()
        }
    }

    fun withSwapchain(stack: MemoryStack, f: (Int) -> Unit) {
        // acquire swapchain
        val acquireInfo = XrSwapchainImageAcquireInfo.calloc(stack)
            .type(XR10.XR_TYPE_SWAPCHAIN_IMAGE_ACQUIRE_INFO)
        val imgIndexPtr = stack.mallocInt(1)
        val acquireRes = XR10.xrAcquireSwapchainImage(
            this.swapchain, acquireInfo, imgIndexPtr
        )
        check(acquireRes == XR10.XR_SUCCESS) { "Failed to acquire swapchain" }
        // wait for swapchain image
        val waitInfo = XrSwapchainImageWaitInfo.calloc(stack)
            .type(XR10.XR_TYPE_SWAPCHAIN_IMAGE_WAIT_INFO)
            .timeout(Long.MAX_VALUE)
        val waitRes = XR10.xrWaitSwapchainImage(this.swapchain, waitInfo)
        check(waitRes == XR10.XR_SUCCESS) { "Failed to wait for swapchain" }
        // call handler
        f(imgIndexPtr.get(0))
        // release swapchain
        val releaseInfo = XrSwapchainImageReleaseInfo.calloc(stack)
            .type(XR10.XR_TYPE_SWAPCHAIN_IMAGE_RELEASE_INFO)
        val releaseRes = XR10.xrReleaseSwapchainImage(
            this.swapchain, releaseInfo
        )
        check(releaseRes == XR10.XR_SUCCESS) { "Failed to release swapchain" }
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
                .type(XR10.XR_TYPE_COMPOSITION_LAYER_PROJECTION_VIEW)
                .pose(views[eye].pose())
                .fov(views[eye].fov())
                .subImage(subImage)
        }
        val projectionLayer = XrCompositionLayerProjection.calloc(stack)
            .type(XR10.XR_TYPE_COMPOSITION_LAYER_PROJECTION)
            .space(this.space)
            .views(projectionViews)
        val frameEndInfo = XrFrameEndInfo.calloc(stack)
            .type(XR10.XR_TYPE_FRAME_END_INFO)
            .displayTime(predDispTime)
            .environmentBlendMode(XR10.XR_ENVIRONMENT_BLEND_MODE_OPAQUE)
            .layerCount(1)
            .layers(stack.pointers(projectionLayer.address()))
        val submitRes = XR10.xrEndFrame(this.session, frameEndInfo)
        check(submitRes == XR10.XR_SUCCESS) { "Failed to submit frame" }
    }

    fun runLoop(
        update: () -> Unit, 
        render: (Camera, Framebuffer) -> Unit
    ) {
        while(true) {
            MemoryStack.stackPush().use { stack ->
                val predDispTime: Long = this.beginFrame(stack)
                this.pollEvents(stack)
                update()
                this.locateViews(stack, predDispTime)
                val views: XrView.Buffer = this.locateViews(stack, predDispTime)
                this.updateCameras(views)
                this.withSwapchain(stack) { imgIndex ->
                    for(eye in 0..<this.viewCount) {
                        val camera = this.cameras[eye]
                        val framebuffer = this.imageFBOs[imgIndex][eye]
                        render(camera, framebuffer)
                    }
                }
                this.submitFrame(stack, predDispTime, views)
            }
        }
    }

}

fun withVrContext(f: (VrContext) -> Unit): Boolean {
    MemoryStack.stackPush().use { stack ->
        val instance: XrInstance = initOpenXr(stack)
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
        val images: Array<Texture> = getXrSwapchainImages(
            stack, session, viewCount, viewConfig, swapchain
        )
        val imageFBOs: Array<Array<Framebuffer>> = Array(images.size) { imgI ->
            Array(viewCount) { eyeI -> Framebuffer(images[imgI], eyeI) }
        }
        println("Created OpenXR swapchain")
        val space: XrSpace = initXrSpace(stack, session)
        val cameras: Array<Camera> = Array(viewCount) { Camera() }
        println("Created reference space")
        f(VrContext(
            instance, sysId, session, viewCount, 
            swapchain, images, imageFBOs, space, cameras
        ))
    }
    return true
}