package com.example.ex07_pointcloud

import android.content.Context
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import android.view.View
import android.widget.Button
import com.google.ar.core.Session
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

//GLSurfaceView 를 렌더링하는 클래스
class MyGLRenderer(val mContext: MainActivity):GLSurfaceView.Renderer {

	var viewportChange = false

	var mCamera:CameraRenderer
	var mPointCloudRenderer: PointCloudRenderer

	var width = 0
	var height = 0

	init{
		mCamera = CameraRenderer()
		mPointCloudRenderer = PointCloudRenderer()
	}


	///get()  :: textureID 실행시
	// 함수처럼  if(mCamera.mTextures==null) -1 else mCamera.mTextures!![0] 실행한 결과를 준다
	// 즉 처음에는 null 이어서 -1 이지만 init() 이 된 이후에는  mCamera.mTextures!![0] 이 된다
	//만일  val textureID = if(mCamera.mTextures==null) -1 else mCamera.mTextures!![0]
	// 이렇게 하면 mCamera.mTextures 가 null 이 아니어도 mCamera.mTextures!![0] 을 주지 않아
	// get() 으로 하여 호출때마다 null 인지 확인하여 결과를 주게 해야 한다.


	val textureID:Int
		get() = if(mCamera.mTextures==null) -1 else mCamera.mTextures!![0]

	override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
		GLES30.glEnable(GLES30.GL_DEPTH_TEST)  //3차원 입체감을 제공
		GLES30.glClearColor(1f,0.6f,0.6f,1f)
		Log.d("MyGLRenderer 여","onSurfaceCreated")


		mCamera.init()
		mPointCloudRenderer.init()
	}

	//화면크기가 변경시 화면 크기를 가져와 작업
	override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
		this.width = width
		this.height = height

		viewportChange = true

		Log.d("MyGLRenderer 여","onSurfaceChanged")
		GLES30.glViewport(0,0,width,height)

	}

	override fun onDrawFrame(gl: GL10?) {
		//Log.d("MyGLRenderer 여","onDrawFrame")
		GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)

		mContext.preRender()  //그릴때 main의 preRender()를 실행한다.

		GLES30.glDepthMask(false)

		mCamera.draw()

		GLES30.glDepthMask(true)

		mPointCloudRenderer.draw()

	}
}