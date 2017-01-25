package io.xol.chunkstories.physics;

public class Colliders
{
	public static boolean dispatchCollision(Collidable a, Collidable b)
	{
		if(a instanceof CollisionBox)
		{
			if(b instanceof CollisionBox)
			{
				return CollisionBox_CollisionBox((CollisionBox)a, (CollisionBox)b);
			}
		}
		
		throw new UnsupportedOperationException("Doesn't know how to check collision of "+a.getClass().getSimpleName()+" against "+b.getClass().getSimpleName());
	}
	
	public static boolean CollisionBox_CollisionBox(CollisionBox a, CollisionBox b)
	{
		if (a.ypos + a.h <= b.ypos || a.ypos >= b.ypos + b.h || a.xpos + a.xw<= b.xpos || a.xpos >= b.xpos + b.xw || a.zpos + a.zw <= b.zpos || a.zpos >= b.zpos + b.zw)
			return false;
		return true;
	}
}
