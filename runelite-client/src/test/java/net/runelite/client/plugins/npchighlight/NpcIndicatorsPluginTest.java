/*
 * Copyright (c) 2018, Tomas Slusny <slusnucky@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.npchighlight;

import com.google.inject.Guice;
import com.google.inject.testing.fieldbinder.Bind;
import com.google.inject.testing.fieldbinder.BoundFieldModule;
import java.awt.Color;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.MenuAction;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.menus.TestMenuEntry;
import net.runelite.client.ui.components.colorpicker.ColorPickerManager;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.game.npcoverlay.HighlightedNpc;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class NpcIndicatorsPluginTest
{
	@Mock
	@Bind
	private Client client;

	@Mock
	@Bind
	private ScheduledExecutorService executorService;

	@Mock
	@Bind
	private NpcIndicatorsConfig npcIndicatorsConfig;

	@Inject
	private NpcIndicatorsPlugin npcIndicatorsPlugin;

	@Mock
	@Bind
	private OverlayManager overlayManager;

	@Mock
	@Bind
	private ConfigManager configManager;

	@Mock
	@Bind
	private ColorPickerManager colorPickerManager;

	@Before
	public void setUp()
	{
		Guice.createInjector(BoundFieldModule.of(this)).injectMembers(this);
		when(npcIndicatorsConfig.highlightColor()).thenReturn(Color.RED);
	}

	@Test
       public void getHighlights()
       {
               when(npcIndicatorsConfig.getNpcToHighlight()).thenReturn("goblin, , zulrah   , *wyvern, ,");
               final List<NpcMatch> highlightedNpcs = npcIndicatorsPlugin.getHighlights();
               assertEquals("Length of parsed NPCs is incorrect", 3, highlightedNpcs.size());

               final Iterator<NpcMatch> iterator = highlightedNpcs.iterator();
               assertEquals("goblin", iterator.next().getPattern());
               assertEquals("zulrah", iterator.next().getPattern());
               assertEquals("*wyvern", iterator.next().getPattern());
       }

       @Test
       public void parseComplexList()
       {
               String configStr = "<tag col=000000,tag</tag>,col=000000></col, Duc*, *at, <col=FFFFFF>Woman</col>, <tag col=FA9000 npcid=311></tag>, <tag col=80FF00FF npcid=306 drawname=true drawmap=false></tag>, <tag col=CCCCCC drawname=false drawmap=true>Man</col>, <tag col=2AAC00 npcid=8665 drawname=true drawmap=true></tag>, <tag col=FF00CC drawname=true drawmap=false>Goblin</tag>, <tag npcid=6773 drawname=false drawmap=false></tag>";
               when(npcIndicatorsConfig.getNpcToHighlight()).thenReturn(configStr);
               List<NpcMatch> matches = npcIndicatorsPlugin.getHighlights();
               assertEquals(8, matches.size());
               assertEquals("Duc*", matches.get(0).getPattern());
               assertEquals("*at", matches.get(1).getPattern());
               NpcMatch m = matches.get(2);
               assertEquals("Woman", m.getPattern());
               assertEquals(ColorUtil.fromHex("FFFFFF"), m.getColor());
               m = matches.get(3);
               assertNull(m.getPattern());
               assertEquals(Integer.valueOf(311), m.getNpcId());
               assertEquals(ColorUtil.fromHex("FA9000"), m.getColor());
               m = matches.get(4);
               assertNull(m.getPattern());
               assertEquals(Integer.valueOf(306), m.getNpcId());
               assertEquals(ColorUtil.fromHex("80FF00FF"), m.getColor());
               assertEquals(Boolean.TRUE, m.getDrawName());
               assertEquals(Boolean.FALSE, m.getDrawMap());
               m = matches.get(5);
               assertNull(m.getPattern());
               assertEquals(Integer.valueOf(8665), m.getNpcId());
               assertEquals(ColorUtil.fromHex("2AAC00"), m.getColor());
               assertEquals(Boolean.TRUE, m.getDrawName());
               assertEquals(Boolean.TRUE, m.getDrawMap());
               m = matches.get(6);
               assertEquals("Goblin", m.getPattern());
               assertEquals(ColorUtil.fromHex("FF00CC"), m.getColor());
               assertEquals(Boolean.TRUE, m.getDrawName());
               assertEquals(Boolean.FALSE, m.getDrawMap());
               m = matches.get(7);
               assertNull(m.getPattern());
               assertEquals(Integer.valueOf(6773), m.getNpcId());
               assertNull(m.getColor());
               assertEquals(Boolean.FALSE, m.getDrawName());
               assertEquals(Boolean.FALSE, m.getDrawMap());
       }

       @Test
       public void parseLegacyColTag()
       {
               when(npcIndicatorsConfig.getNpcToHighlight()).thenReturn("<col=FFFFFF>Man</col>");
               List<NpcMatch> matches = npcIndicatorsPlugin.getHighlights();
               assertEquals(1, matches.size());
               NpcMatch match = matches.get(0);
               assertEquals("Man", match.getPattern());
               assertNull(match.getNpcId());
               assertEquals(ColorUtil.fromHex("FFFFFF"), match.getColor());
               assertNull(match.getDrawName());
               assertNull(match.getDrawMap());
       }

       @Test
       public void parseNameTag()
       {
               when(npcIndicatorsConfig.getNpcToHighlight()).thenReturn("<tag col=FFFFFF drawname=true>Man</tag>");
               List<NpcMatch> matches = npcIndicatorsPlugin.getHighlights();
               assertEquals(1, matches.size());
               NpcMatch match = matches.get(0);
               assertEquals("Man", match.getPattern());
               assertNull(match.getNpcId());
               assertEquals(ColorUtil.fromHex("FFFFFF"), match.getColor());
               assertEquals(Boolean.TRUE, match.getDrawName());
       }

       @Test
       public void invalidTagDiscarded()
       {
               when(npcIndicatorsConfig.getNpcToHighlight()).thenReturn("<tag col=80FF00D4 npcid=3, man, cow");
               List<NpcMatch> matches = npcIndicatorsPlugin.getHighlights();
               assertEquals(2, matches.size());
               assertEquals("man", matches.get(0).getPattern());
               assertEquals("cow", matches.get(1).getPattern());
       }

       @Test
       public void parseAttributesNpcIdIgnoresText()
       {
               when(npcIndicatorsConfig.getNpcToHighlight()).thenReturn("<tag col=00ffff npcid=1234 drawname=false>Rubble</tag>");
               final List<NpcMatch> highlightedNpcs = npcIndicatorsPlugin.getHighlights();
               assertEquals(1, highlightedNpcs.size());
               NpcMatch match = highlightedNpcs.get(0);
               assertNull(match.getPattern());
               assertEquals(Integer.valueOf(1234), match.getNpcId());
               assertEquals(ColorUtil.fromHex("00ffff"), match.getColor());
               assertEquals(Boolean.FALSE, match.getDrawName());
       }

       @Test
       public void highlightByNpcIdOverride()
       {
               when(npcIndicatorsConfig.getNpcToHighlight()).thenReturn("<tag col=ff0000 npcid=1 drawname=true drawmap=false>Wrong</tag>");
               when(npcIndicatorsConfig.drawNames()).thenReturn(false);
               when(npcIndicatorsConfig.drawMinimapNames()).thenReturn(true);

               npcIndicatorsPlugin.rebuild();

               NPC npc = mock(NPC.class);
               when(npc.getName()).thenReturn("Any");
               when(npc.getId()).thenReturn(1);

               npcIndicatorsPlugin.onNpcSpawned(new NpcSpawned(npc));

               HighlightedNpc highlighted = npcIndicatorsPlugin.getHighlightedNpcs().get(npc);
               assertTrue(highlighted.isName());
               assertFalse(highlighted.isNameOnMinimap());
               assertEquals(ColorUtil.fromHex("ff0000"), highlighted.getHighlightColor());
       }

       @Test
       public void perNpcDrawNameOverrideTakesPrecedence()
       {
               when(npcIndicatorsConfig.getNpcToHighlight()).thenReturn("<tag color=00ffff drawname=true>Goblin</tag>");
               when(npcIndicatorsConfig.drawNames()).thenReturn(true);

               npcIndicatorsPlugin.rebuild();

               // simulate context-menu override
               when(configManager.getConfiguration(NpcIndicatorsConfig.GROUP, "drawname_1", Boolean.class)).thenReturn(false);

               NPC npc = mock(NPC.class);
               when(npc.getName()).thenReturn("Goblin");
               when(npc.getId()).thenReturn(1);

               npcIndicatorsPlugin.onNpcSpawned(new NpcSpawned(npc));

               HighlightedNpc highlighted = npcIndicatorsPlugin.getHighlightedNpcs().get(npc);
               assertFalse(highlighted.isName());
       }

	@Test
	public void testDeadNpcMenuHighlight()
	{
		when(npcIndicatorsConfig.getNpcToHighlight()).thenReturn("goblin");
		when(npcIndicatorsConfig.deadNpcMenuColor()).thenReturn(Color.RED);

		npcIndicatorsPlugin.rebuild();

		NPCComposition comp = mock(NPCComposition.class);
		when(comp.getActions()).thenReturn(new String[] {"Attack"});
		NPC npc = mock(NPC.class);
		when(npc.getName()).thenReturn("Goblin");
		when(npc.isDead()).thenReturn(true);
		when(npc.getTransformedComposition()).thenReturn(comp);
		npcIndicatorsPlugin.onNpcSpawned(new NpcSpawned(npc));

		TestMenuEntry entry = new TestMenuEntry();
		entry.setTarget("Goblin");
		entry.setIdentifier(MenuAction.NPC_FIRST_OPTION.getId());
		entry.setActor(npc);

		MenuEntryAdded menuEntryAdded = new MenuEntryAdded(entry);
		npcIndicatorsPlugin.onMenuEntryAdded(menuEntryAdded);

		assertEquals("<col=ff0000>Goblin", entry.getTarget()); // red
	}

	@Test
	public void testNpcMenuHighlightBoth()
	{
		when(npcIndicatorsConfig.getNpcToHighlight()).thenReturn("goblin");
		when(npcIndicatorsConfig.highlightMenuStyle()).thenReturn(NpcIndicatorsConfig.MenuHighlightStyle.BOTH);
		when(npcIndicatorsConfig.highlightColor()).thenReturn(Color.BLUE);

		npcIndicatorsPlugin.rebuild();

		NPC npc = mock(NPC.class);
		when(npc.getName()).thenReturn("Goblin");
		npcIndicatorsPlugin.onNpcSpawned(new NpcSpawned(npc));

		TestMenuEntry entry = new TestMenuEntry();
		entry.setTarget("Goblin");
		entry.setIdentifier(MenuAction.NPC_FIRST_OPTION.getId());
		entry.setActor(npc);

		MenuEntryAdded menuEntryAdded = new MenuEntryAdded(entry);
		npcIndicatorsPlugin.onMenuEntryAdded(menuEntryAdded);

		assertEquals("<col=0000ff>Goblin", entry.getTarget()); // blue
	}

	@Test
	public void testNpcMenuHighlightName()
	{
		when(npcIndicatorsConfig.getNpcToHighlight()).thenReturn("ram");
		when(npcIndicatorsConfig.highlightMenuStyle()).thenReturn(NpcIndicatorsConfig.MenuHighlightStyle.NAME);
		when(npcIndicatorsConfig.highlightColor()).thenReturn(Color.BLUE);

		npcIndicatorsPlugin.rebuild();

		NPC npc = mock(NPC.class);
		when(npc.getName()).thenReturn("Ram");
		npcIndicatorsPlugin.onNpcSpawned(new NpcSpawned(npc));

		TestMenuEntry entry = new TestMenuEntry();
		entry.setTarget("<col=ffff00>Ram<col=ff00>  (level-2)");
		entry.setIdentifier(MenuAction.NPC_FIRST_OPTION.getId());
		entry.setActor(npc);

		MenuEntryAdded menuEntryAdded = new MenuEntryAdded(entry);
		npcIndicatorsPlugin.onMenuEntryAdded(menuEntryAdded);

		assertEquals("<col=0000ff>Ram<col=ff00>  (level-2)", entry.getTarget()); // blue name
	}

	@Test
	public void testNpcMenuHighlightLevel()
	{
		when(npcIndicatorsConfig.getNpcToHighlight()).thenReturn("ram");
		when(npcIndicatorsConfig.highlightMenuStyle()).thenReturn(NpcIndicatorsConfig.MenuHighlightStyle.LEVEL);
		when(npcIndicatorsConfig.highlightColor()).thenReturn(Color.BLUE);

		npcIndicatorsPlugin.rebuild();

		NPC npc = mock(NPC.class);
		when(npc.getName()).thenReturn("Ram");
		npcIndicatorsPlugin.onNpcSpawned(new NpcSpawned(npc));

		TestMenuEntry entry = new TestMenuEntry();
		entry.setTarget("<col=ffff00>Ram<col=ff00>  (level-2)");
		entry.setIdentifier(MenuAction.NPC_FIRST_OPTION.getId());
		entry.setActor(npc);

		MenuEntryAdded menuEntryAdded = new MenuEntryAdded(entry);
		npcIndicatorsPlugin.onMenuEntryAdded(menuEntryAdded);

		assertEquals("<col=ffff00>Ram<col=0000ff>  (level-2)", entry.getTarget()); // blue level
	}

	@Test
	public void taggedNpcChanges()
	{
		when(npcIndicatorsConfig.getNpcToHighlight()).thenReturn("Joseph");

		npcIndicatorsPlugin.rebuild();

		NPC npc = mock(NPC.class);
		when(npc.getName()).thenReturn("Joseph");
		npcIndicatorsPlugin.onNpcSpawned(new NpcSpawned(npc));

		assertTrue(npcIndicatorsPlugin.getHighlightedNpcs().containsKey(npc));

		when(npc.getName()).thenReturn("Werewolf");
		npcIndicatorsPlugin.onNpcChanged(new NpcChanged(npc, null));

		assertFalse(npcIndicatorsPlugin.getHighlightedNpcs().containsKey(npc));
	}

	@Test
	public void npcChangesToTagged()
	{
		when(npcIndicatorsConfig.getNpcToHighlight()).thenReturn("Werewolf");

		npcIndicatorsPlugin.rebuild();

		NPC npc = mock(NPC.class);
		when(npc.getName()).thenReturn("Joseph");
		npcIndicatorsPlugin.onNpcSpawned(new NpcSpawned(npc));

		assertFalse(npcIndicatorsPlugin.getHighlightedNpcs().containsKey(npc));

		when(npc.getName()).thenReturn("Werewolf");
		npcIndicatorsPlugin.onNpcChanged(new NpcChanged(npc, null));

		assertTrue(npcIndicatorsPlugin.getHighlightedNpcs().containsKey(npc));
	}
}
