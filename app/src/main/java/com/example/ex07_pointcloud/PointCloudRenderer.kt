package com.example.ex07_pointcloud

import android.graphics.Color
import android.opengl.GLES30
import android.opengl.Matrix
import android.util.Log
import com.google.ar.core.PointCloud
import java.util.HashMap

class PointCloudRenderer {

	var mViewMatrix = FloatArray(16)
	var mProjMatrix = FloatArray(16)

	//GPU  점 위치 연산 함수
	var vertexShaderString = """
           uniform mat4 uMVPMatrix; 
           uniform float uPointSize; 
           uniform vec4 uColor; 
           varying vec4 vColor; 
           attribute vec4 aPosition; 
           void main(){ 
           vColor = uColor ; 
           gl_Position = uMVPMatrix * vec4( aPosition.xyz , 1.0) ; 
           gl_PointSize = uPointSize ; 
           }
           """.trimIndent()

	//GPU  점 색상 연산 함수
	var fragmentShaderString = """
           precision mediump float; 
           varying vec4 vColor; 
           void main(){ 
           gl_FragColor = vColor ; 
           }
           """.trimIndent()
	var mVbo: IntArray? = null
	var ccs = HashMap<String, FloatArray>()

	//색상값   -- 주황색 ==> 배열로도 가능
	var color: FloatArray?
	var pointSize = 25f
	var mNumPoints = 0

	//GPU 연산식 번호
	var mProgram = 0

	//초기화
	fun init() {
		mVbo = IntArray(1)
		GLES30.glGenBuffers(1, mVbo, 0)
		GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mVbo!![0])
		GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, 1000 * 16, null, GLES30.GL_DYNAMIC_DRAW)
		GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)


		//점 위치 계산식 변수 번호 지정
		val vertexShader = GLES30.glCreateShader(GLES30.GL_VERTEX_SHADER)
		GLES30.glShaderSource(vertexShader, vertexShaderString)
		GLES30.glCompileShader(vertexShader)


		//점 색상 계산식 변수 번호 지정
		val fragmentShader = GLES30.glCreateShader(GLES30.GL_FRAGMENT_SHADER)
		GLES30.glShaderSource(fragmentShader, fragmentShaderString)
		GLES30.glCompileShader(fragmentShader)
		mProgram = GLES30.glCreateProgram()
		GLES30.glAttachShader(mProgram, vertexShader)
		GLES30.glAttachShader(mProgram, fragmentShader)
		GLES30.glLinkProgram(mProgram) //GPU  계산 번호를 링크를 건다 --> 그릴때 링크된 번호를 불러와 연산한다.
		Log.d("PointCloudRenderer 여", "init() ")
	}

	fun draw() {
		val mMVPMatrix = FloatArray(16)
		Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mViewMatrix, 0)
		GLES30.glUseProgram(mProgram) //링크 건 계산 식을 가져온다.

		//점 정보 변수
		val aPosition = GLES30.glGetAttribLocation(mProgram, "aPosition")
		//색 정보 변수 번호
		val uColor = GLES30.glGetUniformLocation(mProgram, "uColor")
		//메트릭스 정보 변수 변호
		val uMVPMatrix = GLES30.glGetUniformLocation(mProgram, "uMVPMatrix")
		//색 정보 변수 번호
		val uPointSize = GLES30.glGetUniformLocation(mProgram, "uPointSize")
		GLES30.glEnableVertexAttribArray(aPosition) //aPosition 변수번호 사용하는 정보를 배열 형태로 사용한다.
		GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mVbo!![0])


		// Log.d("PointCloudRenderer 여","draw() ");
		//점 정보 넣기
		GLES30.glVertexAttribPointer(
			aPosition,  //배열을 넣을 계산식 변수번호
			4,  //점의 좌표계 갯수
			GLES30.GL_FLOAT,  //점 자료형 float
			false,  //점 정규화 하지 않음
			4 * 4,  // 점 한개에 대한 정보 크기 (x,y,z)* float
			0
		)

		//점크기 넣기
		GLES30.glUniform1f(uPointSize, pointSize)

		//메트릭스 정보 넣기
		GLES30.glUniformMatrix4fv(uMVPMatrix, 1, false, mMVPMatrix, 0)

		//색정보
		//색정보 넣기
		GLES30.glUniform4fv(uColor, 1, color, 0)
		// GLES30.glUniform4f(uColor, 1f, 0f, 0f, 1f);

		//그리는 순서에 따라 그린다.
		GLES30.glDrawArrays(
			GLES30.GL_POINTS,
			0,
			mNumPoints
		)

		//그리기 닫는다.
		GLES30.glDisableVertexAttribArray(aPosition)
		GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
	}

	fun updateProjMatrix(matrix: FloatArray?) {
		System.arraycopy(matrix, 0, mProjMatrix, 0, 16)
	}

	fun updateViewMatrix(matrix: FloatArray?) {
		System.arraycopy(matrix, 0, mViewMatrix, 0, 16)
	}

	fun update(pointCloud: PointCloud) {
		GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, mVbo!![0])
		mNumPoints = pointCloud.points.remaining() / 4
		GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, mNumPoints * 16, pointCloud.points)
		GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)

		// Log.d("PointCloudRenderer 여","update() ");
	}

	fun setColor(cc: Int) {
		Log.d("PointCloudRenderer 여", "" + cc)
		color = floatArrayOf(
			Color.red(cc) / 255f,
			Color.green(cc) / 255f,
			Color.blue(cc) / 255f,
			1.0f
		)
	}

	fun setColor(title: String) {
		Log.d("setColor 여", "" + title)
		color = ccs[title]
	}
	init {
		ccs["빨강"] = floatArrayOf(1.0f, 0f, 0f, 1.0f)
		ccs["노랑"] = floatArrayOf(1.0f, 1.0f, 0f, 1.0f)
		ccs["초록"] = floatArrayOf(0f, 1.0f, 0f, 1.0f)
		ccs["하늘"] = floatArrayOf(0f, 1.0f, 1.0f, 1.0f)
		color = ccs["초록"]
	}
}