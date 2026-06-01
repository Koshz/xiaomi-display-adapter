package com.obabo.xiaomihdmiadapter.projection

import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.graphics.SurfaceTexture
import android.os.Handler
import android.os.Looper
import android.view.Surface
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.atomic.AtomicBoolean
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

@SuppressLint("ViewConstructor")
class StretchGlSurfaceView(
    context: Context,
    captureSpec: CaptureSpec,
    surfaceListener: InputSurfaceListener
) : GLSurfaceView(context) {
    private val stretchRenderer = StretchRenderer(
        captureSpec = captureSpec,
        surfaceListener = surfaceListener,
        requestFrame = { requestRender() }
    )
    private var released = false

    init {
        setEGLContextClientVersion(2)
        preserveEGLContextOnPause = true
        setRenderer(stretchRenderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    fun releaseRenderer() {
        if (released) return
        released = true
        queueEvent { stretchRenderer.release() }
    }

    override fun onDetachedFromWindow() {
        releaseRenderer()
        super.onDetachedFromWindow()
    }
}

private class StretchRenderer(
    private val captureSpec: CaptureSpec,
    private val surfaceListener: InputSurfaceListener,
    private val requestFrame: () -> Unit
) : GLSurfaceView.Renderer {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val frameAvailable = AtomicBoolean(false)
    private val texMatrix = FloatArray(16)
    private var program = 0
    private var textureId = 0
    private var surfaceTexture: SurfaceTexture? = null
    private var inputSurface: Surface? = null
    private var positionHandle = 0
    private var textureHandle = 0
    private var textureMatrixHandle = 0
    private var samplerHandle = 0

    private val vertexBuffer: FloatBuffer = ByteBuffer
        .allocateDirect(VERTICES.size * FLOAT_BYTES)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(VERTICES)
            position(0)
        }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        try {
            if (!CaptureSpecProvider.isLandscape(captureSpec)) {
                surfaceListener.onRendererError(CaptureSpecProvider.LANDSCAPE_REQUIRED_MESSAGE)
                return
            }
            Matrix.setIdentityM(texMatrix, 0)
            program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
            positionHandle = GLES20.glGetAttribLocation(program, "aPosition")
            textureHandle = GLES20.glGetAttribLocation(program, "aTexCoord")
            textureMatrixHandle = GLES20.glGetUniformLocation(program, "uTexMatrix")
            samplerHandle = GLES20.glGetUniformLocation(program, "uTexture")

            textureId = createExternalTexture()
            surfaceTexture = SurfaceTexture(textureId).also { texture ->
                texture.setDefaultBufferSize(captureSpec.width, captureSpec.height)
                texture.setOnFrameAvailableListener(
                    {
                        frameAvailable.set(true)
                        requestFrame()
                    },
                    mainHandler
                )
            }
            inputSurface = Surface(surfaceTexture).also {
                surfaceListener.onInputSurfaceReady(it, captureSpec)
            }
        } catch (throwable: Throwable) {
            surfaceListener.onRendererError("OpenGL renderer failed to start", throwable)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        val texture = surfaceTexture ?: return
        if (frameAvailable.getAndSet(false)) {
            texture.updateTexImage()
            texture.getTransformMatrix(texMatrix)
        }

        GLES20.glUseProgram(program)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glUniform1i(samplerHandle, 0)
        GLES20.glUniformMatrix4fv(textureMatrixHandle, 1, false, texMatrix, 0)

        vertexBuffer.position(0)
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(
            positionHandle,
            2,
            GLES20.GL_FLOAT,
            false,
            STRIDE_BYTES,
            vertexBuffer
        )

        vertexBuffer.position(2)
        GLES20.glEnableVertexAttribArray(textureHandle)
        GLES20.glVertexAttribPointer(
            textureHandle,
            2,
            GLES20.GL_FLOAT,
            false,
            STRIDE_BYTES,
            vertexBuffer
        )

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(textureHandle)
    }

    fun release() {
        surfaceListener.onInputSurfaceDestroyed()
        inputSurface?.release()
        inputSurface = null
        surfaceTexture?.release()
        surfaceTexture = null
        if (textureId != 0) {
            GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
            textureId = 0
        }
        if (program != 0) {
            GLES20.glDeleteProgram(program)
            program = 0
        }
    }

    private fun createExternalTexture(): Int {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        val id = textures[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, id)
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )
        return id
    }

    private fun createProgram(vertexShader: String, fragmentShader: String): Int {
        val vertex = compileShader(GLES20.GL_VERTEX_SHADER, vertexShader)
        val fragment = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader)
        val createdProgram = GLES20.glCreateProgram()
        GLES20.glAttachShader(createdProgram, vertex)
        GLES20.glAttachShader(createdProgram, fragment)
        GLES20.glLinkProgram(createdProgram)
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(createdProgram, GLES20.GL_LINK_STATUS, linkStatus, 0)
        if (linkStatus[0] == 0) {
            val log = GLES20.glGetProgramInfoLog(createdProgram)
            GLES20.glDeleteProgram(createdProgram)
            error("Could not link GLES program: $log")
        }
        GLES20.glDeleteShader(vertex)
        GLES20.glDeleteShader(fragment)
        return createdProgram
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)
        if (compileStatus[0] == 0) {
            val log = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            error("Could not compile GLES shader: $log")
        }
        return shader
    }

    companion object {
        private const val FLOAT_BYTES = 4
        private const val STRIDE_BYTES = 4 * FLOAT_BYTES

        private val VERTICES = floatArrayOf(
            -1f, -1f, 0f, 1f,
            1f, -1f, 1f, 1f,
            -1f, 1f, 0f, 0f,
            1f, 1f, 1f, 0f
        )

        private const val VERTEX_SHADER = """
            attribute vec4 aPosition;
            attribute vec4 aTexCoord;
            uniform mat4 uTexMatrix;
            varying vec2 vTexCoord;
            void main() {
                gl_Position = aPosition;
                vTexCoord = (uTexMatrix * aTexCoord).xy;
            }
        """

        private const val FRAGMENT_SHADER = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            varying vec2 vTexCoord;
            uniform samplerExternalOES uTexture;
            void main() {
                gl_FragColor = texture2D(uTexture, vTexCoord);
            }
        """
    }
}
