/*
 * opsu!dance - fork of opsu! with cursordance auto
 * Copyright (C) 2016 yugecin
 *
 * opsu!dance is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * opsu!dance is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with opsu!dance.  If not, see <http://www.gnu.org/licenses/>.
 */
package yugecin.opsudance.sbv2;

import itdelatrisu.opsu.ui.animations.AnimationEquation;
import org.newdawn.slick.Graphics;
import yugecin.opsudance.sbv2.movers.StoryboardMover;

public interface StoryboardMove {

	void setAnimationEquation(AnimationEquation eq);
	int getAmountOfMovers();
	void add(StoryboardMover mover);
	float[] getPointAt(float t);
	void update(int delta, int x, int y);
	void mousePressed(int x, int y);
	void mouseReleased(int x, int y);
	void recalculateTimes();
	void render(Graphics g);

}
