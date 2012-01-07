/*
 * Copyright © 2011 GlobalMentor, Inc. <http://www.globalmentor.com/>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.globalmentor.android.widget;

import static java.util.Collections.unmodifiableSet;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

import android.content.Context;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ViewConfiguration;

/**
 * A gesture listener that detects flings and ensures that they fall within certain limits, such as velocity, distance, and axis. Child classes should override
 * {@link #onVerifiedFling(MotionEvent, MotionEvent, float, float)}.
 * 
 * Adapted to check in which direction the most distance was traveled.
 * 
 * @author Garret Wilson
 * @author Alex Kuiper
 * 
 * @see <a href="http://www.codeshogun.com/blog/2009/04/16/how-to-implement-swipe-action-in-android/">How to implement Swipe action in Android</a>
 * @see <a href="http://stackoverflow.com/questions/937313/android-basic-gesture-detection">Android - basic gesture detection</a>
 */
public class VerifiedFlingListener extends GestureDetector.SimpleOnGestureListener
{

	enum Axis { HORIZONTAL, VERTICAL }
	
	/** The current context. */
	private final Context context;

	/** @return The current context. */
	protected Context getContext()
	{
		return context;
	}

	/** The supported axes for flinging; a fling will only be verified if it occurs on one of these axes. */
	private Set<Axis> flingAxes = unmodifiableSet(EnumSet.allOf(Axis.class));

	/** @return The supported axes for flinging; a fling will only be verified if it occurs on one of these axes. */
	public Set<Axis> getFlingAxes()
	{
		return flingAxes;
	}

	/**
	 * Sets the supported axes for flinging.
	 * @param axes The supported axes; a fling will only be verified if it occurs on one of these axes.
	 * @throws NullPointerException if the given collection is <code>null</code>.
	 */
	public void setFlingAxes(final Collection<Axis> axes)
	{
		this.flingAxes = unmodifiableSet(EnumSet.copyOf(axes));
	}

	/**
	 * Sets the supported axes.
	 * @param axes The supported axes; a fling will only be verified if it occurs on one of these axes.
	 */
	public void setAxes(final Axis... axes)
	{
		setFlingAxes(Arrays.asList(axes));
	}

	/**
	 * Context constructor.
	 * @param context The current context.
	 * @throws NullPointerException if the given context is <code>null</code>.
	 */
	public VerifiedFlingListener(final Context context)
	{		
		this.context = context;
	}

	/**
	 * {@inheritDoc} This implementation verifies a fling using {@link #isFlingVerified(Axis, float, float)} and, if successful, calls
	 * {@link #onVerifiedFling(MotionEvent, MotionEvent, float, float)}.
	 * <p>
	 * This version should normally not be overridden by child classes; instead, {@link #onVerifiedFling(MotionEvent, MotionEvent, float, float)} should be
	 * overridden instead. If a child class overrides this method, this version must be called at some point.
	 * </p>
	 * @see #onVerifiedFling(MotionEvent, MotionEvent, float, float)
	 */
	@Override
	public boolean onFling(final MotionEvent e1, final MotionEvent e2, float velocityXParam, float velocityYParam)
	{
		if(e1 == null || e2 == null) //if one of the motion events is missing (see http://stackoverflow.com/questions/937313/android-basic-gesture-detection/5641723#5641723 )
		{
			return false;	//ignore the fling
		}
		
		//We work under the assumption here that the greatest distance travelled indicates the intended fling.
		float distanceX = Math.abs( e1.getX() - e2.getX() );
		float distanceY = Math.abs( e1.getY() - e2.getY() );
		
		float velocityX = 0;
		float velocityY = 0;
		
		if(distanceX > distanceY && isFlingVerified(Axis.HORIZONTAL, e2.getX() - e1.getX(), velocityXParam)) //verify the horizontal fling
		{
			velocityX = velocityXParam;
		} else if ( distanceY > distanceX && isFlingVerified(Axis.VERTICAL, e2.getY() - e1.getY(), velocityYParam)) //verify the vertical fling
		{
			velocityY = velocityYParam;
		}
		if(velocityX != 0 || velocityY != 0) //if a fling was verified in at least one direction
		{
			return onVerifiedFling(e1, e2, velocityX, velocityY);
		}
		else
		//if the fling thresholds were not reached
		{
			return false; //ignore the event
		}
	}

	/**
	 * Determines whether the given fling gesture falls within the appropriate limits.
	 * <p>
	 * This version ensures that the given axis is supported and that the fling velocity is within limits.
	 * </p>
	 * @see #getFlingAxes()
	 * @see ViewConfiguration#getScaledMinimumFlingVelocity()
	 * @see ViewConfiguration#getScaledMaximumFlingVelocity()
	 */
	public boolean isFlingVerified(final Axis axis, final float distance, float velocity)
	{
		if(!getFlingAxes().contains(axis)) //if this isn't a supported axis
		{
			return false;
		}
		final ViewConfiguration viewConfiguration = ViewConfiguration.get(getContext());
		final float absoluteVelocity = Math.abs(velocity);
		if(absoluteVelocity <= viewConfiguration.getScaledMinimumFlingVelocity() || absoluteVelocity > viewConfiguration.getScaledMaximumFlingVelocity())
		{
			return false;
		}
		return true;
	}

	/**
	 * Called when a verified fling occurs. Only the velocity of the axis(es) on which fling has been verified will be non-zero.
	 * @param e1 The starting event.
	 * @param e2 The ending event.
	 * @param velocityX The horizontal velocity, or 0 if a page fling was not verified horizontally.
	 * @param velocityY The vertical velocity, or 0 if a page fling was not verified vertically.
	 * @return <code>true</code> if the event was consumed.
	 */
	public boolean onVerifiedFling(final MotionEvent e1, final MotionEvent e2, final float velocityX, final float velocityY)
	{
		return false;
	}
}
