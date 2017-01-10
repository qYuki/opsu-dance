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
package yugecin.opsudance.kernel;

import com.google.inject.AbstractModule;
import yugecin.opsudance.PreStartupInitializer;
import yugecin.opsudance.core.DisplayContainer;
import yugecin.opsudance.core.Demux;
import yugecin.opsudance.states.EmptyRedState;
import yugecin.opsudance.states.EmptyState;
import yugecin.opsudance.core.state.transitions.FadeInTransitionState;
import yugecin.opsudance.core.state.transitions.FadeOutTransitionState;

public class OpsuDanceModule extends AbstractModule {

	protected void configure() {
		bind(InstanceContainer.class).to(InstanceResolver.class);
		bind(PreStartupInitializer.class).asEagerSingleton();
		bind(DisplayContainer.class).asEagerSingleton();
		bind(FadeInTransitionState.class).asEagerSingleton();
		bind(FadeOutTransitionState.class).asEagerSingleton();
		bind(EmptyRedState.class).asEagerSingleton();
		bind(EmptyState.class).asEagerSingleton();
		bind(Demux.class).asEagerSingleton();
	}

}
