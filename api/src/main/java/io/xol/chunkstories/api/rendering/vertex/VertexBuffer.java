package io.xol.chunkstories.api.rendering.vertex;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import io.xol.chunkstories.api.util.concurrency.Fence;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface VertexBuffer {

	/**
	 * Uploads new data to this buffer, replacing former content.<br/>
	 * <u>IF THIS IS CALLED IN THE MAIN THREAD:</u><br/>
	 * * Uploading of data is queued for the next time this object is used in a drawcall ( an attribute source is setup, namely )<br/>
	 * * The apparent size of the object ( getVramUsage() ) is updated immediately<br/>
	 * * Multiple subsequent uploads in one frame can override former ones; given the right conditions a call to uploadData followed by another one then a draw call, the first upload will be ignored altogether and only the latest buffer content will be used<br/>
	 * * Changing the VertexBuffer content does not trigger a flush() in the RenderingInterface, if you want to issue draw calls on this buffer with multiple data per frame you have to issue flush() commands between them to make sure the data is uploaded. More information is on the wiki on this.<br/>
	 * <u>IF THIS IS CALLED IN ANY OTHER THREAD:</u><br/>
	 * * The data is queued for upload on the <b>NEXT</b> frame.<br/>
	 * * Before each frame starts being drawed, the latest buffer content provided by a foreign thread, if such exists, is uploaded, replacing the VerticesObject content and updating it's size.
	 * 
	 * @return <ul><li>A Fence only traversable once data is actually uploaded (if called from not the graphics thread)</li>
	 * 		   <li>An instantly traversable fence, if called from the main graphics thread</li></ul>
	 * 
	 * //@return True if the data was uploaded ( or rather, queued for upload on use ), false if it was deffered to the next frame
	 */
	public Fence uploadData(ByteBuffer dataToUpload);

	/**
	 * Uploads new data to this buffer, replacing former content.<br/>
	 * <u>IF THIS IS CALLED IN THE MAIN THREAD:</u><br/>
	 * * Uploading of data is queued for the next time this object is used in a drawcall ( an attribute source is setup, namely )<br/>
	 * * The apparent size of the object ( getVramUsage() ) is updated immediately<br/>
	 * * Multiple subsequent uploads in one frame can override former ones; given the right conditions a call to uploadData followed by another one then a draw call, the first upload will be ignored altogether and only the latest buffer content will be used<br/>
	 * * Changing the VertexBuffer content does not trigger a flush() in the RenderingInterface, if you want to issue draw calls on this buffer with multiple data per frame you have to issue flush() commands between them to make sure the data is uploaded. More information is on the wiki on this.<br/>
	 * <u>IF THIS IS CALLED IN ANY OTHER THREAD:</u><br/>
	 * * The data is queued for upload on the <b>NEXT</b> frame.<br/>
	 * * Before each frame starts being drawed, the latest buffer content provided by a foreign thread, if such exists, is uploaded, replacing the VerticesObject content and updating it's size.
	 * 
	 * @return True if the data was uploaded ( or rather, queued for upload on use ), false if it was deffered to the next frame
	 */
	public Fence uploadData(FloatBuffer dataToUpload);

	public Fence uploadData(RecyclableByteBuffer dataToUpload);

	/**
	 * Notice : there is no risk of synchronisation issues with an object suddently being destroyed during because actual destruction of the objects only occur at the end of the frame !
	 * 
	 * @return True if data is present and the verticesObject can be drawn
	 */
	public boolean isDataPresent();

	public AttributeSource asAttributeSource(VertexFormat format, int dimensions);

	public AttributeSource asAttributeSource(VertexFormat format, int dimensions, int stride, long offset);

	public long getVramUsage();

	/**
	 * Synchronized, returns true only when it actually deletes the OpenGL buffer
	 * As with textures, a GC mechanism is in place if you forget to delete your buffers !
	 */
	public boolean destroy();

}