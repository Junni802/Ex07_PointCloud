package com.example.ex07_pointcloud

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.hardware.display.DisplayManager
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.core.app.ActivityCompat
import com.google.ar.core.ArCoreApk
import com.google.ar.core.Frame
import com.google.ar.core.Session

class MainActivity : AppCompatActivity() {
	var mSession: Session? = null
	var myGLRenderer:MyGLRenderer? = null
	var myGLView: GLSurfaceView? = null

	var sbR:SeekBar? = null
	var sbG:SeekBar? = null
	var sbB:SeekBar? = null
	var sbA:SeekBar? = null

	var ccTxt:TextView? = null

	//                          가상환경에서 카메라 위치 정보             가상환경에서 카메라가 보고 있는 이미지 (영상)
//                                                                              //실제 화면
	// 실제 카메라 -> mSession -> 가상 카메라 -> MyGLRenderer(onDrawFrame ) ->  CameraRenderer

	@SuppressLint("MissingInflatedId")
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_main)

		requestPermission()

		myGLView = findViewById(R.id.myGLView)
		sbR=findViewById(R.id.sbR)
		sbG=findViewById(R.id.sbG)
		sbB=findViewById(R.id.sbB)
		sbA=findViewById(R.id.sbA)
		ccTxt=findViewById(R.id.ccTxt)

		myGLView!!.setEGLContextClientVersion(3)
		//일시중지시 EGLContext 유지여부
		myGLView!!.preserveEGLContextOnPause=true


		myGLRenderer = MyGLRenderer(this)
		//어떻게 그릴 것인가
		myGLView!!.setRenderer(myGLRenderer)

		//화면 렌더링을 언제 할 것인가 = 렌더러 반복호출하여 장면을 다시 그린다.
		myGLView!!.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

		//화면 변화 감지
		val displayManager = getSystemService(DISPLAY_SERVICE) as DisplayManager

		displayManager.registerDisplayListener(
			object: DisplayManager.DisplayListener{
				override fun onDisplayAdded(displayId: Int) {

				}

				override fun onDisplayRemoved(displayId: Int) {

				}

				override fun onDisplayChanged(displayId: Int) {
					synchronized(this){
						//화면 변경 와
						myGLRenderer!!.viewportChange = true
					}
				}

			}, null
		)

		findViewById<SeekBar>(R.id.seekBar).setOnSeekBarChangeListener(
			object:SeekBar.OnSeekBarChangeListener{
				override fun onProgressChanged(
					seekBar: SeekBar?,
					progress: Int,
					fromUser: Boolean
				) {
					Log.d("seekBar 여",""+progress)
					myGLRenderer!!.mPointCloudRenderer.pointSize = progress.toFloat()
				}

				override fun onStartTrackingTouch(seekBar: SeekBar?) {

				}

				override fun onStopTrackingTouch(seekBar: SeekBar?) {

				}

			}

		)

		val mySbc = MySeekBarChangeListener()

		sbR!!.setOnSeekBarChangeListener(mySbc)
		sbG!!.setOnSeekBarChangeListener(mySbc)
		sbB!!.setOnSeekBarChangeListener(mySbc)
		sbA!!.setOnSeekBarChangeListener(mySbc)
	}



	inner class MySeekBarChangeListener:SeekBar.OnSeekBarChangeListener{
		override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
			//Log.d("mySeekBar 여", "$progress")
			val cc = Color.argb(
				sbA!!.progress.toFloat()/255,
				sbR!!.progress.toFloat()/255,
				sbG!!.progress.toFloat()/255,
				sbB!!.progress.toFloat()/255
			)
			ccTxt!!.setBackgroundColor(cc)
			myGLRenderer!!.mPointCloudRenderer.setColor(cc)
		}

		override fun onStartTrackingTouch(seekBar: SeekBar?) {

		}

		override fun onStopTrackingTouch(seekBar: SeekBar?) {

		}

	}

	inner class set

	fun requestPermission(){
		ActivityCompat.requestPermissions(
			this,
			arrayOf(Manifest.permission.CAMERA),
			1234
		)
	}

	override fun onResume() {
		super.onResume()

		if(ArCoreApk.getInstance().requestInstall(this,true) ==
			ArCoreApk.InstallStatus.INSTALLED  ){
			mSession = Session(this)

			mSession!!.resume()

			Log.d("mSession 여","${mSession}")
		}
		myGLView!!.onResume()
	}

	override fun onPause() {
		super.onPause()

		mSession!!.pause()
		myGLView!!.onPause()
	}

	fun preRender(){
		// Log.d("preRender 여","gogo")

		//화면이 변환되었다면
		if(myGLRenderer!!.viewportChange){

			//회전상태 확인
			val display = windowManager.defaultDisplay

			//세션의 화면 정보 갱신
			//myGLRenderer!!.updateSession(mSession!!, display.rotation)
			mSession!!.setDisplayGeometry(display.rotation, myGLRenderer!!.width, myGLRenderer!!.height)
			//화면 변환 해제
			myGLRenderer!!.viewportChange = false

		}

		//이미 실제 카메라를 세션에서 적용
		// 렌더러에서 사용하도록 지정 --> CameraRenderer로 사용하도록 ID 설정
		mSession!!.setCameraTextureName(myGLRenderer!!.textureID)

		var frame: Frame? = null

		try {
			frame = mSession!!.update()
		}catch (e:Exception){

		}

		if(frame!=null) {  //frame이 null 이 되는 경우가 있어서 null이 아닐때만 실행
			myGLRenderer!!.mCamera.transformDisplayGeometry(frame!!)
		}

		//특징점을 획득한다.
		val pointCloud = frame!!.acquirePointCloud()

		myGLRenderer!!.mPointCloudRenderer.update(pointCloud)

		pointCloud.release()

		var mViewMatrix = FloatArray(16)
		var mProjMatrix = FloatArray(16)

		//frame의 camera의 viewMatrix ==> mViewMatrix에 대입입
		frame!!.camera.getViewMatrix(mViewMatrix,0)

		//frame의 camera의 projectionMatrix ==> mProjMatrix 대입입    near     far
		frame!!.camera.getProjectionMatrix(mProjMatrix,0, 0.01f, 100f)

		myGLRenderer!!.mPointCloudRenderer.updateViewMatrix(mViewMatrix)
		myGLRenderer!!.mPointCloudRenderer.updateProjMatrix(mProjMatrix)

	}

	//text로 변경
	fun colorGo(v:View){
		val btn = v as Button

		//Log.d("colorGo 여",btn.text.toString())
		myGLRenderer!!.mPointCloudRenderer.setColor(btn.text.toString())

		when(btn.text.toString()){
			"빨강" -> {
				sbR!!.progress = 255
				sbG!!.progress = 0
				sbB!!.progress = 0
			}
			"노랑" -> {
				sbR!!.progress = 255
				sbG!!.progress = 255
				sbB!!.progress = 0
			}
		}
	}

	//배경색으로 변경
	fun colorBackGo(v:View){
		val btn = v as Button

		val cc:ColorDrawable = btn.background as ColorDrawable

		Log.d("colorGo 여","${cc.color}")
		myGLRenderer!!.mPointCloudRenderer.setColor(cc.color)

		when(btn.id){
			R.id.buttonG -> {
				sbR!!.progress = 0
				sbG!!.progress = 255
				sbB!!.progress = 0
			}
			R.id.buttonS -> {
				sbR!!.progress = 0
				sbG!!.progress = 255
				sbB!!.progress = 255
			}
		}

	}

}