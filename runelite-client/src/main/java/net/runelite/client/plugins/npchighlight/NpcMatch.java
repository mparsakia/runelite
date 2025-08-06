/*
 * Copyright (c) 2024, [codex]
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

import java.awt.Color;
import lombok.Value;
import net.runelite.client.util.ColorUtil;
import net.runelite.client.util.Text;

/**
 * Parsed representation of a single entry from the NPCs to highlight
 * configuration. Supports additional attributes such as npc id and
 * draw name overrides.
 */
@Value
class NpcMatch
{
       /** Pattern used for name matching, may contain wildcards. */
       String pattern;
       /** Optional NPC id to match. */
       Integer npcId;
       /** Optional highlight colour. */
       Color color;
       /** Optional override for drawing the name in the scene. */
       Boolean drawName;
       /** Optional override for drawing the name on the minimap. */
       Boolean drawMap;

       static NpcMatch parse(String config)
       {
               String trimmed = config.trim();

               // Discard obvious malformed tokens such as truncated tags
               if (trimmed.contains("<") && !trimmed.contains(">"))
               {
                       return null;
               }

               if (trimmed.startsWith("<tag") && trimmed.contains(">"))
               {
                       int end = trimmed.indexOf('>');
                       String attrsPart = trimmed.substring("<tag".length(), end).trim();
                       String content = trimmed.substring(end + 1);

                       if (!content.endsWith("</tag>"))
                       {
                               return null; // malformed tag
                       }
                       content = content.substring(0, content.length() - "</tag>".length());

                       String pattern = Text.removeTags(content).trim();
                       if (pattern.isEmpty())
                       {
                               pattern = null;
                       }

                       Integer npcId = null;
                       Color color = null;
                       Boolean drawName = null;
                       Boolean drawMap = null;

                       String[] attrs = attrsPart.isEmpty() ? new String[0] : attrsPart.split("\\s+");
                       for (String attr : attrs)
                       {
                               String[] kv = attr.split("=", 2);
                               if (kv.length < 2)
                               {
                                       continue;
                               }

                               switch (kv[0].toLowerCase())
                               {
                                       case "color":
                                               try
                                               {
                                                       color = ColorUtil.fromHex(kv[1]);
                                               }
                                               catch (IllegalArgumentException ignored)
                                               {
                                               }
                                               break;
                                       case "npcid":
                                               try
                                               {
                                                       npcId = Integer.parseInt(kv[1]);
                                               }
                                               catch (NumberFormatException ignored)
                                               {
                                               }
                                               break;
                                       case "drawname":
                                               drawName = Boolean.parseBoolean(kv[1]);
                                               break;
                                       case "drawmap":
                                               drawMap = Boolean.parseBoolean(kv[1]);
                                               break;
                               }
                       }

                       if (npcId != null)
                       {
                               // When targeting by id the inner text is ignored
                               pattern = null;
                       }

                       if (npcId == null && pattern == null)
                       {
                               // No match criteria
                               return null;
                       }

                       return new NpcMatch(pattern, npcId, color, drawName, drawMap);
               }

               String pattern = Text.removeTags(trimmed).trim();
               if (pattern.isEmpty())
               {
                       return null;
               }
               return new NpcMatch(pattern, null, null, null, null);
       }
}

