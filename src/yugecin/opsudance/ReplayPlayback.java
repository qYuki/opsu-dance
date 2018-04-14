/*
 * opsu!dance - fork of opsu! with cursordance auto
 * Copyright (C) 2017 yugecin
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
package yugecin.opsudance;

import itdelatrisu.opsu.GameData;
import itdelatrisu.opsu.beatmap.Beatmap;
import itdelatrisu.opsu.beatmap.HitObject;
import itdelatrisu.opsu.objects.GameObject;
import itdelatrisu.opsu.objects.curves.Curve;
import itdelatrisu.opsu.replay.Replay;
import itdelatrisu.opsu.replay.ReplayFrame;
import itdelatrisu.opsu.ui.Cursor;
import itdelatrisu.opsu.ui.Fonts;
import itdelatrisu.opsu.ui.animations.AnimationEquation;
import org.newdawn.slick.Color;
import org.newdawn.slick.Graphics;
import org.newdawn.slick.Image;
import yugecin.opsudance.core.DisplayContainer;
import yugecin.opsudance.core.Entrypoint;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

public class ReplayPlayback {

	private final DisplayContainer container;
	private final HitData hitdata;
	public final Replay replay;
	public ReplayFrame currentFrame;
	public ReplayFrame nextFrame;
	private int frameIndex;
	public Color color;
	public Cursor cursor;
	private int keydelay[];
	public static final int SQSIZE = 15;
	public static final int UNITHEIGHT = SQSIZE + 5;
	private boolean hr;
	private String player;
	private String mods;
	private int playerwidth;
	private int textwidth;

	public GameObject[] gameObjects;
	private int objectIndex = 0;
	private int lastkeys = 0;
	private Image hitImage;
	private int hitImageTimer = 0;
	public GData gdata = new GData();
	private boolean missed;

	private static final Color missedColor = new Color(0.4f, 0.4f, 0.4f, 1f);

	public ReplayPlayback(DisplayContainer container, Replay replay, HitData hitdata, Color color) {
		this.container = container;
		this.replay = replay;
		this.hitdata = hitdata;
		resetFrameIndex();
		this.color = color;
		Color cursorcolor = new Color(color);
		//cursorcolor.a = 0.5f;
		cursor = new Cursor(cursorcolor);
		keydelay = new int[4];
		this.player = replay.playerName;
		this.textwidth = Fonts.SMALLBOLD.getWidth(this.player);
		this.playerwidth = this.textwidth;
		this.mods = "";
		if ((replay.mods & 0x1) > 0) {
			this.mods += "NF";
		}
		if ((replay.mods & 0x2) > 0) {
			this.mods += "EZ";
		}
		if ((replay.mods & 0x8) > 0 && (replay.mods & 0x200) == 0) {
			this.mods += "HD";
		}
		if ((replay.mods & 0x10) > 0) {
			this.mods += "HR";
			hr = true;
		}
		if ((replay.mods & 0x20) > 0) {
			this.mods += "SD";
		}
		if ((replay.mods & 0x40) > 0) {
			this.mods += "DT";
		}
		if ((replay.mods & 0x80) > 0) {
			this.mods += "RL";
		}
		if ((replay.mods & 0x100) > 0) {
			this.mods += "HT";
		}
		if ((replay.mods & 0x200) > 0) {
			this.mods += "NC";
		}
		if ((replay.mods & 0x400) > 0) {
			this.mods += "FL";
		}
		if ((replay.mods & 0x4000) > 0) {
			this.mods += "PF";
		}
		if (this.mods.length() > 0) {
			this.mods = " +" + this.mods;
			this.textwidth += Fonts.SMALLBOLD.getWidth(this.mods);
		}
	}

	public void resetFrameIndex() {
		frameIndex = 0;
		currentFrame = replay.frames[frameIndex++];
		nextFrame = replay.frames[frameIndex];
	}

	private void sendKeys(Beatmap beatmap, int trackPosition) {
		if (objectIndex >= gameObjects.length)  // nothing to do here
			return;

		HitObject hitObject = beatmap.objects[objectIndex];

		// circles
		if (hitObject.isCircle() && gameObjects[objectIndex].mousePressed(currentFrame.getScaledX(), currentFrame.getScaledY(), trackPosition))
			objectIndex++;  // circle hit

			// sliders
		else if (hitObject.isSlider())
			gameObjects[objectIndex].mousePressed(currentFrame.getScaledX(), currentFrame.getScaledY(), trackPosition);
	}

	private void update(int trackPosition, Beatmap beatmap, int[] hitResultOffset, int delta) {
		boolean keyPressed = currentFrame.getKeys() != ReplayFrame.KEY_NONE;
		while (objectIndex < gameObjects.length && trackPosition > beatmap.objects[objectIndex].getTime()) {
			// check if we've already passed the next object's start time
			boolean overlap = (objectIndex + 1 < gameObjects.length &&
				trackPosition > beatmap.objects[objectIndex + 1].getTime() - hitResultOffset[GameData.HIT_50]);

			// update hit object and check completion status
			if (gameObjects[objectIndex].update(overlap, delta, currentFrame.getScaledX(), currentFrame.getScaledY(), keyPressed, trackPosition)) {
				objectIndex++;  // done, so increment object index
			} else
				break;
		}
	}

	private int HITIMAGETIMEREXPAND = 200;
	private int HITIMAGETIMERFADESTART = 500;
	private int HITIMAGETIMERFADEEND = 700;
	private float HITIMAGETIMERFADEDELTA = HITIMAGETIMERFADEEND - HITIMAGETIMERFADESTART;
	private int HITIMAGEDEADFADE = 10000;
	private float SHRINKTIME = 500f;
	private void showHitImage(int renderdelta, float ypos) {
		if (hitImage == null) {
			return;
		}

		hitImageTimer += renderdelta;
		if (!missed && hitImageTimer > HITIMAGETIMERFADEEND) {
			hitImage = null;
			return;
		}

		Color color = new Color(1f, 1f, 1f, 1f);
		if (!missed && hitImageTimer > HITIMAGETIMERFADESTART) {
			color.a = (HITIMAGETIMERFADEEND - hitImageTimer) / HITIMAGETIMERFADEDELTA;
		}
		if (missed) {
			if (hitImageTimer > HITIMAGEDEADFADE) {
				this.color.a = color.a = 0f;
			} else {
				this.color.a = color.a = 1f - AnimationEquation.IN_CIRC.calc((float) hitImageTimer / HITIMAGEDEADFADE);
			}
		}
		float scale = 1f;
		float offset = 0f;
		if (hitImageTimer < HITIMAGETIMEREXPAND) {
			scale = AnimationEquation.OUT_EXPO.calc((float) hitImageTimer / HITIMAGETIMEREXPAND);
			offset = UNITHEIGHT / 2f * (1f - scale);
		}
		hitImage.draw(SQSIZE * 5 + textwidth + SQSIZE + offset, ypos + offset, scale, color);
	}

	public float getHeight() {
		if (hitImageTimer < HITIMAGEDEADFADE) {
			return UNITHEIGHT;
		}
		if (hitImageTimer >= HITIMAGEDEADFADE + SHRINKTIME) {
			return 0f;
		}
		return UNITHEIGHT * (1f - AnimationEquation.OUT_QUART.calc((hitImageTimer - HITIMAGEDEADFADE) / SHRINKTIME));
	}

	public void render(Beatmap beatmap, int[] hitResultOffset, int renderdelta, Graphics g, float ypos, int time) {
		/*
		if (objectIndex >= gameObjects.length) {
			return;
		}

		while (nextFrame != null && nextFrame.getTime() < time) {
			currentFrame = nextFrame;

			int keys = currentFrame.getKeys();
			int deltaKeys = (keys & ~lastkeys);  // keys that turned on
			if (deltaKeys != ReplayFrame.KEY_NONE) { // send a key press
				sendKeys(beatmap, currentFrame.getTime());
			} else if (keys == lastkeys) {
				update(time, beatmap, hitResultOffset, currentFrame.getTimeDiff());
			}
			lastkeys = keys;

			processKeys();
			frameIndex++;
			if (frameIndex >= replay.frames.length) {
				nextFrame = null;
				continue;
			}
			nextFrame = replay.frames[frameIndex];
		}
		processKeys();
		*/
		g.setColor(color);
		if (!missed) {
			for (int i = 0; i < 4; i++) {
				if (keydelay[i] > 0) {
					g.fillRect(SQSIZE * i, ypos + 5, SQSIZE, SQSIZE);
				}
				keydelay[i] -= renderdelta;
			}
		}
		Fonts.SMALLBOLD.drawString(SQSIZE * 5, ypos, this.player, color);
		if (!this.mods.isEmpty()) {
			Fonts.SMALLBOLD.drawString(SQSIZE * 5 + playerwidth, ypos, this.mods, new Color(1f, 1f, 1f, color.a));
		}
		showHitImage(renderdelta, ypos);
		if (missed) {
			return;
		}
		int y = currentFrame.getScaledY();
		if (hr) {
			y = container.height - y;
		}
		cursor.setCursorPosition(renderdelta, currentFrame.getScaledX(), y);
		cursor.draw(false);
	}

	private void processKeys() {
		int keys = currentFrame.getKeys();
		int KEY_DELAY = 10;
		if ((keys & 5) == 5) {
			keydelay[0] = KEY_DELAY;
		}
		if ((keys & 10) == 10) {
			keydelay[1] = KEY_DELAY;
		}
		if ((keys ^ 5) == 4) {
			keydelay[2] = KEY_DELAY;
		}
		if ((keys ^ 10) == 8) {
			keydelay[3] = KEY_DELAY;
		}
	}

	public class GData extends GameData {

		public GData() {
			super();
			this.loadImages();
		}

		@Override
		public void sendSliderRepeatResult(int time, float x, float y, Color color, Curve curve, HitObjectType type) {
			// ?
		}

		@Override
		public void sendSliderStartResult(int time, float x, float y, Color color, Color mirrorColor, boolean expand) {
			// ?
		}

		@Override
		public void sendSliderTickResult(int time, int result, float x, float y, HitObject hitObject, int repeat) {
			if (result == HIT_SLIDER30 || result == HIT_SLIDER10) {
				incrementComboStreak();
			}
		}

		@Override
		public void sendHitResult(int time, int result, float x, float y, Color color, boolean end, HitObject hitObject, HitObjectType hitResultType, boolean expand, int repeat, Curve curve, boolean sliderHeldToEnd) {
			sendHitResult(time, result, x, y, color, end, hitObject, hitResultType, expand, repeat, curve, sliderHeldToEnd, true);
		}

		@Override
		public void sendHitResult(int time, int result, float x, float y, Color color, boolean end, HitObject hitObject, HitObjectType hitResultType, boolean expand, int repeat, Curve curve, boolean sliderHeldToEnd, boolean handleResult) {
			if (curve == null || sliderHeldToEnd) {
				incrementComboStreak();
			}

			if (missed || result == HIT_300) {
				return;
			}

			missed = this.getComboStreak() > replay.combo;
			if (missed) {
				result = HIT_MISS;
			}

			if (result == HIT_MISS) {
				if (!missed) {
					result = HIT_50;
				} else {
					ReplayPlayback.this.color = new Color(missedColor);
				}
			}

			if (result < hitResults.length) {
				hitImageTimer = 0;
				hitImage = hitResults[result].getScaledCopy(SQSIZE + 5, SQSIZE + 5);
			}
		}

		@Override
		public void addHitError(int time, int x, int y, int timeDiff) {
			//?
		}
	}

	public static class HitData {

		int combobreaktime = -1;
		LinkedList<Integer> time100 = new LinkedList();
		LinkedList<Integer> time50 = new LinkedList();
		LinkedList<AccData> acc = new LinkedList();

		public HitData(File file) {
			try (InputStream in = new FileInputStream(file)) {
				int lasttime = -1;
				int lastcombo = 0;
				int last100 = 0;
				int last50 = 0;
				while (true) {
					byte[] time = new byte[4];
					int rd = in.read(time);
					if (rd == 0) {
						break;
					}
					if (rd != 4) {
						throw new RuntimeException();
					}
					byte[] _time = { time[3], time[2], time[1], time[0] };
					lasttime = ByteBuffer.wrap(_time).getInt();
					int type = in.read();
					if (type == -1) {
						throw new RuntimeException();
					}
					if (in.read(time) != 4) {
						throw new RuntimeException();
					}
					_time = new byte[] { time[3], time[2], time[1], time[0] };
					switch (type) {
					case 1:
						int this100 = ByteBuffer.wrap(_time).getInt();
						spread(time100, lasttime, this100 - last100);
						last100 = this100;
						break;
					case 3:
						break;
					case 5:
						int this50 = ByteBuffer.wrap(_time).getInt();
						spread(time50, lasttime, this50 - last50);
						last50 = this50;
						break;
					case 10:
						acc.add(new AccData(lasttime, ByteBuffer.wrap(_time).getFloat()));
						break;
					case 12:
						int c = ByteBuffer.wrap(_time).getInt();
						if (c < lastcombo) {
							combobreaktime = lasttime;
						} else {
							lastcombo = c;
						}
						break;
					default:
						throw new RuntimeException();
					}
					if (combobreaktime != -1) {
						break;
					}
				}
				if (combobreaktime == -1) {
					combobreaktime = lasttime;
				}
				if (combobreaktime == -1) {
					throw new RuntimeException("nodata");
				}
				Entrypoint.sout(String.format(
					"%s combobreak at %d, lastcombo %d lastacc %f",
					file.getName(),
					combobreaktime,
					lastcombo,
					acc.getLast().acc
				));
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		private void spread(LinkedList<Integer> list, int time, int d) {
			if (list.isEmpty() || d <= 1) {
				list.add(time);
				return;
			}

			int dtime = time - list.getLast();
			int inc = dtime / d;
			int ttime = list.getLast();
			for (int i = 0; i < d; i++) {
				ttime += inc;
				if (i == d - 1) {
					ttime = time;
				}
				list.add(ttime);
			}
		}

	}

	public static class AccData {
		public int time;
		public float acc;
		public AccData(int time, float acc) {
			this.time = time;
			this.acc = acc;
		}
	}

}