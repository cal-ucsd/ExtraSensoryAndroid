#include <pebble.h>
#define WINDOW_HEIGHT 168
#define WINDOW_WIDTH 144
#define TEXT_AREA_HEIGHT 72
#define DATE_AREA_HEIGHT 24
#define TEXTBOX_HEIGHT 2000
#define ALERT_MAX_SIZE 2000
  
#define COMPASS_INVALID_VALUE -100
#define COMPASS_INVALID_TIMESTAMP -1
#define COMPASS_BUFFER_SIZE 50
#define ACC_BUFFER_SIZE 25
  
#define MESSAGE_TYPE_KEY_RECORDING 1
#define MESSAGE_TYPE_KEY_ALERT 2
    
static Window *window;
static Layer *text_area_layer;
static TextLayer *s_output_layer;
static TextLayer *hour_minute_layer;
static TextLayer *weekday_layer;
static TextLayer *date_layer;
static TextLayer *recording_layer;

static char alert_message_buffer[ALERT_MAX_SIZE];
static bool expecting_answer_from_user = false;
static bool allow_vibrate = true;

static int current_compass_position;
static int compass_buffer1[COMPASS_BUFFER_SIZE];
static int compass_buffer2[COMPASS_BUFFER_SIZE];
static int* compass_buffer = compass_buffer1;
static time_t start_time;
static uint16_t start_time_ms;
static int32_t t1;

static void animate_question(int);
static int number_of_pixels;
static void present_message(char *message);

// Key values for AppMessage Dictionary
enum {
	STATUS_KEY = 0,	
	MESSAGE_KEY = 42
};

char *translate_error(AppMessageResult result) {
  switch (result) {
    case APP_MSG_OK: return "APP_MSG_OK";
    case APP_MSG_SEND_TIMEOUT: return "APP_MSG_SEND_TIMEOUT";
    case APP_MSG_SEND_REJECTED: return "APP_MSG_SEND_REJECTED";
    case APP_MSG_NOT_CONNECTED: return "APP_MSG_NOT_CONNECTED";
    case APP_MSG_APP_NOT_RUNNING: return "APP_MSG_APP_NOT_RUNNING";
    case APP_MSG_INVALID_ARGS: return "APP_MSG_INVALID_ARGS";
    case APP_MSG_BUSY: return "APP_MSG_BUSY";
    case APP_MSG_BUFFER_OVERFLOW: return "APP_MSG_BUFFER_OVERFLOW";
    case APP_MSG_ALREADY_RELEASED: return "APP_MSG_ALREADY_RELEASED";
    case APP_MSG_CALLBACK_ALREADY_REGISTERED: return "APP_MSG_CALLBACK_ALREADY_REGISTERED";
    case APP_MSG_CALLBACK_NOT_REGISTERED: return "APP_MSG_CALLBACK_NOT_REGISTERED";
    case APP_MSG_OUT_OF_MEMORY: return "APP_MSG_OUT_OF_MEMORY";
    case APP_MSG_CLOSED: return "APP_MSG_CLOSED";
    case APP_MSG_INTERNAL_ERROR: return "APP_MSG_INTERNAL_ERROR";
    default: return "UNKNOWN ERROR";
  }
}

void set_expecting_answer(bool expecting) {
  expecting_answer_from_user = expecting;
}

void switch_allow_vibrate() {
  allow_vibrate = !allow_vibrate;
  present_message(allow_vibrate ? "Vibration on" : "Vibration off");
}

void anim_stopped_handler(Animation *animation, bool finished, void *context) {
  // Free the animation
  //property_animation_destroy(s_box_animation);
  animation_destroy(animation);
  
  // Schedule the reverse animation, unless the app is exiting
  if (finished) {
//    number_of_pixels = -number_of_pixels;
    animate_question(number_of_pixels);
  }
}

void animate_question(int pixels_to_scroll_by) {
  int len = pixels_to_scroll_by;
  if(pixels_to_scroll_by > 0) {
    len = pixels_to_scroll_by + 24;
  }
  else  {
    len = pixels_to_scroll_by - 24;
  }
  GRect start_frame = GRect(0, (pixels_to_scroll_by < 0 ? 0 : -len), WINDOW_WIDTH, TEXTBOX_HEIGHT);
  GRect finish_frame =  GRect(0, (pixels_to_scroll_by < 0 ? len : 0), WINDOW_WIDTH, TEXTBOX_HEIGHT);
  
  PropertyAnimation *box_animation = property_animation_create_layer_frame(text_layer_get_layer(s_output_layer), &start_frame, &finish_frame);
  animation_set_handlers((Animation*)box_animation, (AnimationHandlers) {
    .stopped = anim_stopped_handler
  }, NULL);
  animation_set_duration((Animation*)box_animation, abs(pixels_to_scroll_by) * 35); // delay is proportional to text size
  animation_set_curve((Animation*)box_animation, AnimationCurveLinear);  // setting equal speed animation
  animation_set_delay((Animation*)box_animation, 3000); //initial delay of 3 seconds to let user start reading quote

  animation_schedule((Animation*)box_animation);
}

// Write message to buffer & send
static AppMessageResult send_message(char* message){
  
	DictionaryIterator *iter;
	AppMessageResult result;
	result = app_message_outbox_begin(&iter);
  if (result != APP_MSG_OK) { 
    APP_LOG(APP_LOG_LEVEL_DEBUG, "Failed to start sending message: %s", translate_error(result));
    return result; 
  }
	dict_write_cstring(iter, MESSAGE_KEY, message);
	
	dict_write_end(iter);
  result = app_message_outbox_send();
  APP_LOG(APP_LOG_LEVEL_DEBUG, "Tried sending message: %s", translate_error(result));
  return result;
}
static AppMessageResult send_acc_data(AccelData *d) {
// Prepare dictionary
  DictionaryIterator *iter;
  AppMessageResult result;
  result = app_message_outbox_begin(&iter);
  if (result != APP_MSG_OK) { 
    APP_LOG(APP_LOG_LEVEL_DEBUG, "Failed to start sending acceleration data: %s", translate_error(result));
    return result; 
  }
  // Add the acceleration values:
  char temp[24];
  uint64_t timestamp = d->timestamp;
  int dt = (int)(timestamp - t1);
  snprintf(temp, 24, "%d", dt);
  dict_write_cstring(iter, 0, temp);
  for(uint8_t i = 0; i < ACC_BUFFER_SIZE; i++, d++) {
    snprintf(temp, 24, "%d,%d,%d", d->x, d->y, d->z);
    dict_write_cstring(iter, i+1, temp);
  }
  dict_write_end(iter);
  result = app_message_outbox_send();
  APP_LOG(APP_LOG_LEVEL_DEBUG, "Tried sending acceleration data: %s", translate_error(result));
  return result;
}
static void send_compass_data(int *compass_buffer) {
// Prepare dictionary
  DictionaryIterator *iterator;
  app_message_outbox_begin(&iterator);
  
  char temp_compass[24];
  for(uint8_t j = 0; j < COMPASS_BUFFER_SIZE-1; j=j+2) {
    snprintf(temp_compass, 24, "%d:%d", compass_buffer[j], compass_buffer[j+1]);
    dict_write_cstring(iterator, j/2, temp_compass);
  }
  app_message_outbox_send();
}

static void clean_compass_buffer() {
  for (uint8_t i = 0; i < COMPASS_BUFFER_SIZE; i ++) {
    compass_buffer[i] = -77;
  }
  current_compass_position = 0;
}

// data handler to recieve accel data from watch
static void data_handler(AccelData *data, uint32_t num_samples) {
  if(num_samples < ACC_BUFFER_SIZE){
    APP_LOG(APP_LOG_LEVEL_DEBUG, "Data handler - not enough data samples! %d", (int)num_samples);
    return;
  }
  AppMessageResult result = send_acc_data(data);
  //if (result != APP_MSG_OK) {
  //  psleep(300);
  //  result = send_acc_data(data);
  //}
}

static void compass_handler(CompassHeadingData data) {
  // Allocate a static output buffer
  //static char s_buffer[32];

  // Determine status of the compass
  switch (data.compass_status) {
    // Compass data is not yet valid
    case CompassStatusDataInvalid:
      APP_LOG(APP_LOG_LEVEL_DEBUG, "Compass data invalid");
      break;

    // Compass is currently calibrating, but a heading is available
    case CompassStatusCalibrating:
      APP_LOG(APP_LOG_LEVEL_DEBUG, "Compass calibrating\nHeading: %d", TRIGANGLE_TO_DEG((int)data.true_heading));
      break;
    // Compass data is ready for use, write the heading in to the buffer
    case CompassStatusCalibrated: 
      //APP_LOG(APP_LOG_LEVEL_DEBUG, "Compass calibrated\nHeading: %d", TRIGANGLE_TO_DEG((int)data.true_heading));
      compass_buffer[current_compass_position] = 0;//TRIGANGLE_TO_DEG((int)data.true_heading);
    time_t sec;
    uint16_t ms;
    int32_t t2, dt;
    time_ms(&sec, &ms);
    t2 = (int32_t)1000*(int32_t)sec + (int32_t)ms;

    // dt is the time spent in milliseconds
    dt = t2 - t1;
    
      //APP_LOG(APP_LOG_LEVEL_DEBUG, "t = %d", (int)dt);
      compass_buffer[current_compass_position] = dt;
      current_compass_position++;
    if (current_compass_position >= COMPASS_BUFFER_SIZE) {
      APP_LOG(APP_LOG_LEVEL_DEBUG, "Compass buffer full too early");
    }
      compass_buffer[current_compass_position] = TRIGANGLE_TO_DEG((int)data.true_heading);
      current_compass_position++;
    break;

    // CompassStatus is unknown
    default:
      APP_LOG(APP_LOG_LEVEL_DEBUG, "Unknown CompassStatus: %d", data.compass_status);
      break;
  }
  if (current_compass_position >= COMPASS_BUFFER_SIZE) {
    // Time to send the values:
    APP_LOG(APP_LOG_LEVEL_DEBUG, "Compass buffer full: %d, %d, %d, ...", compass_buffer[0], compass_buffer[1], compass_buffer[2]);
    send_compass_data(compass_buffer);
    //switch to other buffer
    if (compass_buffer == compass_buffer2) {
      compass_buffer = compass_buffer1;
    } else {
      compass_buffer = compass_buffer2;
    }
    current_compass_position = 0;
    //APP_LOG(APP_LOG_LEVEL_DEBUG, "Compass buffer emptied: %d, %d, %d, ...", compass_buffer[0], compass_buffer[1], compass_buffer[2]);
  }
}

static void send_remaining_compass_data(){
// Prepare dictionary
  DictionaryIterator *iterator;
  app_message_outbox_begin(&iterator);
  
  char temp_compass[24];
  for(uint8_t j = 0; j < current_compass_position-1; j=j+2) {
    snprintf(temp_compass, 24, "%d:%d", compass_buffer[j], compass_buffer[j+1]);
    dict_write_cstring(iterator, j/2 , temp_compass);
  }
  app_message_outbox_send();
}

void turnOnSensorCollection() {
  // subscribe to the accel data 
  APP_LOG(APP_LOG_LEVEL_DEBUG, "turnOnSensorCollection");
  uint32_t num_samples = ACC_BUFFER_SIZE;
  accel_service_set_sampling_rate(ACCEL_SAMPLING_25HZ);
  accel_data_service_subscribe(num_samples, data_handler);
  
  // subscribe to compass data:
  //clean_compass_buffer();
  compass_service_subscribe(compass_handler);
  compass_service_set_heading_filter(TRIG_MAX_ANGLE / 360);
  
  time_ms(&start_time, &start_time_ms);
  t1 = (int32_t)1000*(int32_t)start_time + (int32_t)start_time_ms;
}

void turnOffSensorCollection() {
  send_remaining_compass_data();
  accel_data_service_unsubscribe();
  compass_service_unsubscribe();
}

// Called when a message is received from iPhone
static void in_received_handler(DictionaryIterator *iterator, void *context) {
    // Get the first pair
  Tuple *t = dict_read_first(iterator);

  // Process all pairs present
  while(t != NULL) {
    // Process this pair's key
    switch (t->key) {
      case MESSAGE_TYPE_KEY_RECORDING:
        APP_LOG(APP_LOG_LEVEL_INFO, "KEY DATA: %d", (int)t->key);
        APP_LOG(APP_LOG_LEVEL_INFO, "KEY_DATA received with value %s", t->value->cstring);
        if(strcmp(t->value->cstring, "TURN OFF") == 0)
        {
          turnOffSensorCollection();
          layer_set_hidden((Layer *)recording_layer, true);
        }
        if(strcmp(t->value->cstring, "TURN ON") == 0)
        {
          turnOnSensorCollection();
          layer_set_hidden((Layer *)recording_layer, false);
        }
        break;
      case MESSAGE_TYPE_KEY_ALERT:
        APP_LOG(APP_LOG_LEVEL_INFO, "KEY DATA: %d", (int)t->key);
        APP_LOG(APP_LOG_LEVEL_INFO, "KEY_DATA received with value %s", t->value->cstring);
        strncpy(alert_message_buffer,t->value->cstring,ALERT_MAX_SIZE);
        present_message(alert_message_buffer);
        if (allow_vibrate) {
          vibes_short_pulse();
        }
        // Mark that we're expecting answer from user:
        set_expecting_answer(true);
        break;
    }

    // Get next pair, if any
    t = dict_read_next(iterator);
  }
}

// Called when an outgoing message from watch app was sent successfully
static void out_sent_handler(DictionaryIterator *sent, void *context) {
    // outgoing message was delivered
  APP_LOG(APP_LOG_LEVEL_DEBUG, "Size of Dict sent: %d", (int)dict_size(sent));
}

// Called when an incoming message from iPhone is dropped
static void in_dropped_handler(AppMessageResult reason, void *context) {
  // incoming message was dropped
     APP_LOG(APP_LOG_LEVEL_DEBUG, "In dropped: %i - %s", reason, translate_error(reason));
}

// Called when an outgoing message from watch app is dropped
static void out_failed_handler(DictionaryIterator *failed, AppMessageResult reason, void *context) {
  // outgoing message failed
  APP_LOG(APP_LOG_LEVEL_DEBUG, "Out dropped: %i - %s", reason, translate_error(reason));//, (int)dict_read_first(failed)->key);
}

void bluetooth_handler(bool connected) {
  if (connected) {
    present_message("Waiting for new message...");
  } else {
    present_message("Phone is not connected");
    layer_set_hidden((Layer *)recording_layer, true);
    accel_data_service_unsubscribe();
    compass_service_unsubscribe();
  }
}

// when message is recieved, click up button to confirm activity
static void up_click_handler(ClickRecognizerRef recognizer, void *context) {
  animation_unschedule_all();
  APP_LOG(APP_LOG_LEVEL_DEBUG, "UP Button Pressed!");
  if (expecting_answer_from_user) {
    AppMessageResult result = send_message("YES");
    int count = 0;
    while ((result != APP_MSG_OK) & (count < 3)) {
      psleep(300); //miliseconds
      result = send_message("YES");
      count++;
    }
    if (result == APP_MSG_OK) {
      set_expecting_answer(false);
      present_message("Confirmed as yes! Waiting for new message...");
    }
  }
}

// when message is recieved, click down button to confirm it was not activity
static void down_click_handler(ClickRecognizerRef recognizer, void *context) {
  animation_unschedule_all();
  APP_LOG(APP_LOG_LEVEL_DEBUG, "DOWN Button Pressed!");  
  if (expecting_answer_from_user) {
    AppMessageResult result = send_message("NO");
    int count = 0;
    while ((result != APP_MSG_OK) & (count < 3)) {
      psleep(300); //miliseconds
      result = send_message("YES");
      count++;
    }
    if (result == APP_MSG_OK) {
      set_expecting_answer(false);
      present_message("Not now! Waiting for new message...");
    }
  }
  else {
    switch_allow_vibrate();    
  }
}

static void click_config_provider(void *context) {
  // Register the ClickHandlers
  window_single_click_subscribe(BUTTON_ID_UP, up_click_handler);
  window_single_click_subscribe(BUTTON_ID_DOWN, down_click_handler);
}

static TextLayer* create_message_text_layer(int16_t x_origin,int16_t y_origin) {
  TextLayer *newTextLayer = text_layer_create(GRect(x_origin, y_origin, WINDOW_WIDTH, TEXTBOX_HEIGHT));
  text_layer_set_background_color(newTextLayer, GColorBlack);
  text_layer_set_text_color(newTextLayer, GColorWhite);
  text_layer_set_font(newTextLayer, fonts_get_system_font(FONT_KEY_GOTHIC_24));
  
  return newTextLayer;
}

static void present_message(char *message) {
  // Set the frame of the layer to be regular:
  GRect start_frame = GRect(0, 0, WINDOW_WIDTH, TEXTBOX_HEIGHT);
  layer_set_frame((Layer *)s_output_layer, start_frame);
  
  // Set the content of the layer:
  text_layer_set_text(s_output_layer,message);
  // if height of quote > height of window, initiate animation to scroll
  GSize text_size = text_layer_get_content_size(s_output_layer);
  int number_of_pixels = TEXT_AREA_HEIGHT - text_size.h;
  if (number_of_pixels < 0) {
    animate_question(number_of_pixels);
  }
}

static void handle_minute_tick(struct tm* tick_time, TimeUnits units_changed) {
  static char hour_minute_text[] = "00:00";
  strftime(hour_minute_text, sizeof(hour_minute_text), "%R", tick_time);
  text_layer_set_text(hour_minute_layer, hour_minute_text);
  
  static char weekday_text[] = "wednesday";
  strftime(weekday_text, sizeof(weekday_text), "%A", tick_time);
  text_layer_set_text(weekday_layer, weekday_text);
  
  static char date_text[] = "JUL 31";
  strftime(date_text, sizeof(date_text), "%b %d", tick_time);
  text_layer_set_text(date_layer, date_text);
}

static void main_window_load(Window *window) {
  Layer *window_layer = window_get_root_layer(window);

  // Text area:
  text_area_layer = layer_create(GRect(0,0,WINDOW_WIDTH,TEXT_AREA_HEIGHT));
  layer_add_child(window_layer,text_area_layer);
  s_output_layer = create_message_text_layer(0, 0);
  layer_add_child(text_area_layer, text_layer_get_layer(s_output_layer));

  present_message("Welcome to ESW!");

  // Digital watch area:
  int16_t y_origin_date = TEXT_AREA_HEIGHT;
  int16_t weekday_area_width = WINDOW_WIDTH / 2;
  weekday_layer = text_layer_create(GRect(0, y_origin_date, weekday_area_width, DATE_AREA_HEIGHT));
  layer_add_child(window_layer, text_layer_get_layer(weekday_layer));
  
  int16_t date_width = WINDOW_WIDTH / 3;
  date_layer = text_layer_create(GRect(weekday_area_width, y_origin_date, date_width, DATE_AREA_HEIGHT));
  layer_add_child(window_layer, text_layer_get_layer(date_layer));
  
  int16_t x_origin_rec = weekday_area_width + date_width;
  recording_layer = text_layer_create(GRect(x_origin_rec, y_origin_date, WINDOW_WIDTH-x_origin_rec, DATE_AREA_HEIGHT));
  text_layer_set_text_alignment(recording_layer, GTextAlignmentRight);
  layer_set_hidden((Layer *)recording_layer, true);
  text_layer_set_text(recording_layer, "REC");
  layer_add_child(window_layer, text_layer_get_layer(recording_layer));
  
  
  int16_t y_origin_hour_minute = y_origin_date + DATE_AREA_HEIGHT;
  int16_t height_hour_minute = WINDOW_HEIGHT - y_origin_hour_minute;
  hour_minute_layer = text_layer_create(GRect(0, y_origin_hour_minute, WINDOW_WIDTH, height_hour_minute));
  text_layer_set_text_alignment(hour_minute_layer, GTextAlignmentCenter);
  text_layer_set_font(hour_minute_layer, fonts_get_system_font(FONT_KEY_BITHAM_42_BOLD));
  layer_add_child(window_layer, text_layer_get_layer(hour_minute_layer));

  /*
  // Action bar:
  action_bar_layer = action_bar_layer_create();
  action_bar_layer_add_to_window(action_bar_layer, window);
  */
}

static void main_window_unload(Window *window) {
  // Destroy output TextLayer
  text_layer_destroy(s_output_layer);
  layer_destroy(text_area_layer);
  text_layer_destroy(hour_minute_layer);
  text_layer_destroy(weekday_layer);
  text_layer_destroy(date_layer);
  text_layer_destroy(recording_layer);
//  action_bar_layer_destroy(action_bar_layer);
}

void init(void) {
//  prepare_icons();
  // create main window element and assign to pointr
	window = window_create();
  
  // Set handlers to manage the elements inside the Window
  window_set_window_handlers(window, (WindowHandlers) {
    .load = main_window_load,
    .unload = main_window_unload
  });
  
  window_set_click_config_provider(window, click_config_provider);
      
  // Show the Window on the watch, with animated=true
	window_stack_push(window, true);
  
	// Register AppMessage handlers
	app_message_register_inbox_received(in_received_handler); 
	app_message_register_inbox_dropped(in_dropped_handler); 
  app_message_register_outbox_sent(out_sent_handler);
	app_message_register_outbox_failed(out_failed_handler);
  
  // Register bluetooth connection handler
  bluetooth_connection_service_subscribe(bluetooth_handler);
  
  // subscribe to the accel data 
  //uint32_t num_samples = 25;
  //accel_data_service_subscribe(num_samples, data_handler);
  //accel_service_set_sampling_rate(ACCEL_SAMPLING_25HZ);

	app_message_open(app_message_inbox_size_maximum(), app_message_outbox_size_maximum());
  app_comm_set_sniff_interval(SNIFF_INTERVAL_REDUCED);
  
  // Register time response:
  tick_timer_service_subscribe(MINUTE_UNIT,handle_minute_tick);
  time_t now = time(NULL);
  struct tm *current_time = localtime(&now);
  handle_minute_tick(current_time,MINUTE_UNIT);

}

void deinit(void) {
	app_message_deregister_callbacks();
  accel_data_service_unsubscribe();
  compass_service_unsubscribe();
  app_comm_set_sniff_interval(SNIFF_INTERVAL_NORMAL);
  tick_timer_service_unsubscribe();
	window_destroy(window);
//  destroy_icons();
}

int main( void ) {
	init();
  APP_LOG(APP_LOG_LEVEL_DEBUG, "Done initializing, pushed window: %p", window);
	app_event_loop();
	deinit();
}