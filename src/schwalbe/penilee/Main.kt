
package schwalbe.penilee

import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL
import org.lwjgl.openxr.*
import org.lwjgl.openxr.KHROpenGLEnable.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil.NULL

fun initWindow(): Long {
    check(glfwInit()) { "Failed to initialize GLFW" }
    glfwDefaultWindowHints()
    glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3)
    glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3)
    glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_COMPAT_PROFILE)
    glfwWindowHint(GLFW_VISIBLE, GLFW_TRUE)
    val window: Long = glfwCreateWindow(800, 600, "Penilee Island Railway", NULL, NULL)
        ?: throw RuntimeException("Failed to create window")
    glfwMakeContextCurrent(window)
    GL.createCapabilities()
    glfwShowWindow(window)
    return window
}

fun initOpenXr(stack: MemoryStack): XrInstance {
    val appInfo = XrApplicationInfo.calloc(stack).apply {
        applicationName(stack.UTF8("Penilee Island Railway"))
        applicationVersion(1)
        engineName(stack.UTF8("LWJGL"))
        engineVersion(1)
        apiVersion(XR10.XR_CURRENT_API_VERSION)
    }
    val extensions = stack.mallocPointer(1)
    extensions.put(stack.UTF8(KHROpenGLEnable.XR_KHR_OPENGL_ENABLE_EXTENSION_NAME))
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
    check(XR10.xrGetSystem(instance, sysInfo, sysId) == XR10.XR_SUCCESS) {
        "No VR system found"
    }
    return sysId.get(0)
}

fun initXrSession(stack: MemoryStack, instance: XrInstance, sysId: Long): XrSession {
    // determine graphics requirenments
    val graphicsRequirements = XrGraphicsRequirementsOpenGLKHR.calloc(stack).apply {
        type(KHROpenGLEnable.XR_TYPE_GRAPHICS_REQUIREMENTS_OPENGL_KHR)
        next(NULL)
    }
    val requirementsResult = KHROpenGLEnable.xrGetOpenGLGraphicsRequirementsKHR(instance, sysId, graphicsRequirements)
    check(requirementsResult == XR10.XR_SUCCESS) { "Failed to get OpenGL graphics requirements: $requirementsResult" }
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
    val createResult = XR10.xrCreateSession(instance, sessionCreateInfo, sessionPtr)
    check(createResult == XR10.XR_SUCCESS) { "Failed to create XR session: $createResult" }
    val session = XrSession(sessionPtr.get(0), instance)
    // begin session
    val sessionBeginInfo = XrSessionBeginInfo.calloc(stack).apply {
        type(XR10.XR_TYPE_SESSION_BEGIN_INFO)
        primaryViewConfigurationType(XR10.XR_VIEW_CONFIGURATION_TYPE_PRIMARY_STEREO)
    }
    check(XR10.xrBeginSession(session, sessionBeginInfo) == XR10.XR_SUCCESS) {
        "Failed to begin session"
    }
    return session
}

fun main() {
    val window: Long = initWindow()
    println("Created window")
    MemoryStack.stackPush().use { stack ->
        val instance: XrInstance = initOpenXr(stack)
        println("Created OpenXR instance")
        val sysId: Long = selectXrRuntime(stack, instance)
        println("Selected OpenXR runtime with ID ${sysId}")
        val session: XrSession = initXrSession(stack, instance, sysId)
        println("Created OpenXR session")
        while(!glfwWindowShouldClose(window)) {
            glfwPollEvents()
            glfwSwapBuffers(window)
        }
    }
}