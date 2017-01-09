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
package yugecin.opsudance.states.transitions;

import com.google.inject.Inject;
import yugecin.opsudance.core.Demux;
import yugecin.opsudance.core.DisplayContainer;

public class FadeInTransitionState extends FadeTransitionState {

	private final Demux demux;

	@Inject
	public FadeInTransitionState(DisplayContainer container, Demux demux) {
		super(container, 300);
		this.demux = demux;
	}

	@Override
	protected float getMaskAlphaLevel(float fadeProgress) {
		return 1f - fadeProgress;
	}

	@Override
	public void enter() {
		super.enter();
		applicableState.enter();
	}

	@Override
	protected void onTransitionFinished() {
		demux.switchStateNow(applicableState);
	}

}
