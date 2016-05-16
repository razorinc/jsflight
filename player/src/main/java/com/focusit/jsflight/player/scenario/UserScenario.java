package com.focusit.jsflight.player.scenario;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import com.focusit.jsflight.player.config.Configuration;
import com.focusit.jsflight.player.constants.EventType;
import com.focusit.jsflight.player.input.Events;
import com.focusit.jsflight.player.input.FileInput;
import com.focusit.jsflight.player.script.PlayerScriptProcessor;
import com.focusit.script.jmeter.JMeterJSFlightBridge;
import com.focusit.script.player.PlayerContext;

/**
 * Recorded scenario encapsulation: parses file, plays the scenario by step, modifies the scenario, saves to a disk.
 *
 * @author Denis V. Kirpichenkov
 *
 */
public class UserScenario
{
    // TODO add classpath for scripts
    private static HashMap<String, JSONObject> lastEvents = new HashMap<>();
    private volatile int position = 0;
    private List<JSONObject> events = new ArrayList<>();
    private String postProcessScenarioScript = "";
    private List<Boolean> checks = new ArrayList<>();
    private PlayerContext context = new PlayerContext();
    private Configuration configuration = new Configuration();

    public void checkStep(int position)
    {

    }

    public void copyStep(int position)
    {
        String event = events.get(position).toString();
        JSONObject clone = new JSONObject(event);
        events.add(position, clone);
    }

    public void deleteStep(int position)
    {
        events.remove(position);
    }

    public Configuration getConfiguration()
    {
        return configuration;
    }

    public void setConfiguration(Configuration configuration)
    {
        this.configuration = configuration;
    }

    public PlayerContext getContext()
    {
        return context;
    }

    public int getPosition()
    {
        return position;
    }

    public void setPosition(int position)
    {
        this.position = position;
    }

    public JSONObject getPrevEvent(JSONObject event)
    {
        return lastEvents.get(getTagForEvent(event));
    }

    public String getScenarioFilename()
    {
        return "";
    }

    public JSONObject getStepAt(int position)
    {
        return events.get(position);
    }

    public int getStepsCount()
    {
        return events.size();
    }

    public String getTagForEvent(JSONObject event)
    {
        String tag = "null";
        if (event.has(JMeterJSFlightBridge.TAG_FIELD))
        {
            tag = event.getString(JMeterJSFlightBridge.TAG_FIELD);
        }

        return tag;
    }

    public String getTargetForEvent(JSONObject event)
    {
        if (event.has("target2"))
        {
            return event.getString("target2");
        }
        if (!event.has("target1"))
        {
            return "";
        }
        JSONArray array = event.getJSONArray("target1");
        if (array.isNull(0))
        {
            return "";
        }

        String target = array.getJSONObject(0).getString("getxp");
        return target;
    }

    public boolean isEventBad(JSONObject event)
    {
        return event.getString("type").equals(EventType.SCRIPT) ? false
                : !event.has("target") || event.get("target") == null || event.get("target") == JSONObject.NULL;
    }

    public boolean isEventIgnored(String eventType)
    {
        return eventType.equalsIgnoreCase(EventType.XHR) || eventType.equalsIgnoreCase(EventType.HASH_CHANGE)
                || (!eventType.equalsIgnoreCase(EventType.CLICK) && !eventType.equalsIgnoreCase(EventType.KEY_PRESS)
                        && !eventType.equalsIgnoreCase(EventType.KEY_UP)
                        && !eventType.equalsIgnoreCase(EventType.KEY_DOWN)
                        && !eventType.equalsIgnoreCase(EventType.SCROLL_EMULATION)
                        && !eventType.equalsIgnoreCase(EventType.MOUSEWHEEL)
                        && !eventType.equalsIgnoreCase(EventType.MOUSEDOWN) && !eventType.equals(EventType.SCRIPT));
    }

    public boolean isStepDuplicates(String script, JSONObject event)
    {
        JSONObject prev = getPrevEvent(event);

        if (prev != null)
        {
            return new PlayerScriptProcessor(this).executeDuplicateHandlerScript(script, event, prev);
        }

        return false;
    }

    public void next()
    {
        checks.set(position, true);
        setPosition(getPosition() + 1);
        if (getPosition() == getStepsCount())
        {
            for (int i = 0; i < getPosition(); i++)
            {
                checks.set(i, false);
            }
            position = 0;
        }
    }

    public void parse(String filename) throws IOException
    {
        events.clear();
        events.addAll(new Events().parse(FileInput.getContent(filename)));
        context.reset();
    }

    public void parseNextLine(String filename) throws IOException
    {
        events.clear();
        List<JSONObject> result = new Events().parse(FileInput.getLineContent(filename));
        if (result != null)
        {
            events.addAll(result);
        }
    }

    public long postProcessScenario()
    {
        if (!postProcessScenarioScript.isEmpty())
        {
            new PlayerScriptProcessor(this).postProcessScenario(postProcessScenarioScript, events);
        }
        checks = new ArrayList<>(getStepsCount());
        for (int i = 0; i < getStepsCount(); i++)
        {
            checks.add(new Boolean(false));
        }

        long secs = 0;

        if (getStepsCount() > 0)
        {
            secs = getStepAt(getStepsCount() - 1).getBigDecimal("timestamp").longValue()
                    - getStepAt(0).getBigDecimal("timestamp").longValue();
        }

        return secs;
    }

    public void prev()
    {
        if (getPosition() > 0)
        {
            setPosition(getPosition() - 1);
        }
    }

    public void rewind()
    {
        checks.stream().forEach(it -> {
            it = Boolean.FALSE;
        });
        context.reset();
        setPosition(0);
    }

    public void runPostProcessor(String script)
    {
        PlayerScriptProcessor engine = new PlayerScriptProcessor(this);
        engine.testPostProcess(script, events);
    }

    public void saveScenario(String filename) throws IOException
    {
        FileInput.saveEvents(events, filename);
    }

    public void setPostProcessScenarioScript(String postProcessScenarioScript)
    {
        this.postProcessScenarioScript = postProcessScenarioScript;
    }

    public void skip()
    {
        setPosition(getPosition() + 1);
    }

    public void updatePrevEvent(JSONObject event)
    {
        lastEvents.put(getTagForEvent(event), event);
    }

    public void updateStep(int position, JSONObject event)
    {
        events.set(position, event);
    }
}
