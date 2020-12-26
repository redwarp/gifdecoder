use jni::{
    objects::{JClass, JObject, JValue, ReleaseMode},
    sys::{jboolean, jbyteArray, jint, jintArray, JNI_TRUE},
    JNIEnv,
};

const MAX_STACK_SIZE: usize = 4096;

#[no_mangle]
#[allow(non_snake_case)]
pub unsafe extern "C" fn Java_net_redwarp_gif_decoder_lzw_NativeLzwDecoder_decode(
    env: JNIEnv,
    _class: JClass,
    image_data: jbyteArray,
    destination: jbyteArray,
    pixel_count: jint,
) {
    let rust_image_data = env
        .get_auto_byte_array_elements(image_data, ReleaseMode::CopyBack)
        .unwrap();
    let image_data_length = env.get_array_length(image_data).unwrap() as usize;
    let converted_image_data =
        std::slice::from_raw_parts(rust_image_data.as_ptr() as *const u8, image_data_length);

    let rust_destination = env
        .get_auto_byte_array_elements(destination, ReleaseMode::CopyBack)
        .unwrap();
    let destination_length = env.get_array_length(destination).unwrap() as usize;
    let mut converted_destination =
        std::slice::from_raw_parts_mut(rust_destination.as_ptr() as *mut u8, destination_length);

    decode(
        &converted_image_data,
        &mut converted_destination,
        pixel_count as usize,
    );
}

#[no_mangle]
#[allow(non_snake_case)]
pub unsafe extern "C" fn Java_net_redwarp_gif_decoder_lzw_NativeLzwDecoder_access(
    env: JNIEnv,
    _class: JClass,
    bridge: JObject,
) {
    let field = env.get_field(bridge, "interlaced", "Z").unwrap();
    let field_value = field.z().unwrap();

    let test = JValue::Bool(!field_value as jboolean);

    env.set_field(bridge, "interlaced", "Z", test).unwrap();
}

#[no_mangle]
#[allow(non_snake_case)]
pub unsafe extern "C" fn Java_net_redwarp_gif_decoder_lzw_NativeLzwDecoder_fillPixels(
    env: JNIEnv,
    _class: JClass,
    pixels: jintArray,
    color_data: jbyteArray,
    color_table: jintArray,
    transparent_color_index: jint,
    image_width: jint,
    frame_width: jint,
    frame_height: jint,
    offset_x: jint,
    offset_y: jint,
    interlaced: jboolean,
) {
    let pixels_length = env.get_array_length(pixels).unwrap() as usize;
    let auto_pixels = env
        .get_auto_primitive_array_critical(pixels, ReleaseMode::CopyBack)
        .unwrap();
    let mut pixels =
        std::slice::from_raw_parts_mut(auto_pixels.as_ptr() as *mut u32, pixels_length);

    let color_data_length = env.get_array_length(color_data).unwrap() as usize;
    let auto_color_data = env
        .get_auto_primitive_array_critical(color_data, ReleaseMode::CopyBack)
        .unwrap();
    let color_data =
        std::slice::from_raw_parts(auto_color_data.as_ptr() as *const u8, color_data_length);

    let color_table_length = env.get_array_length(color_table).unwrap() as usize;
    let auto_color_table = env
        .get_auto_primitive_array_critical(color_table, ReleaseMode::CopyBack)
        .unwrap();
    let color_table =
        std::slice::from_raw_parts(auto_color_table.as_ptr() as *const u32, color_table_length);

    fill_pixel(
        &mut pixels,
        &color_data,
        &color_table,
        transparent_color_index as usize,
        image_width as u32,
        frame_width as u32,
        frame_height as u32,
        offset_x as u32,
        offset_y as u32,
        interlaced == JNI_TRUE,
    )
}

#[no_mangle]
#[allow(non_snake_case)]
pub unsafe extern "C" fn Java_net_redwarp_gif_decoder_lzw_NativeLzwDecoder_decodeFull(
    env: JNIEnv,
    _class: JClass,
    image_data: jbyteArray,
    scratch: jbyteArray,
    pixels: jintArray,
    color_table: jintArray,
    transparent_color_index: jint,
    image_width: jint,
    frame_width: jint,
    frame_height: jint,
    offset_x: jint,
    offset_y: jint,
    interlaced: jboolean,
) {
    let image_data_length = env.get_array_length(image_data).unwrap() as usize;
    let auto_image_data = env
        .get_auto_primitive_array_critical(image_data, ReleaseMode::CopyBack)
        .unwrap();
    let image_data =
        std::slice::from_raw_parts(auto_image_data.as_ptr() as *const u8, image_data_length);

    let scratch_length = env.get_array_length(scratch).unwrap() as usize;
    let auto_scratch = env
        .get_auto_primitive_array_critical(scratch, ReleaseMode::CopyBack)
        .unwrap();
    let mut scratch =
        std::slice::from_raw_parts_mut(auto_scratch.as_ptr() as *mut u8, scratch_length);

    decode(image_data, scratch, (frame_width * frame_height) as usize);

    let pixels_length = env.get_array_length(pixels).unwrap() as usize;
    let auto_pixels = env
        .get_auto_primitive_array_critical(pixels, ReleaseMode::CopyBack)
        .unwrap();
    let mut pixels =
        std::slice::from_raw_parts_mut(auto_pixels.as_ptr() as *mut u32, pixels_length);

    let color_table_length = env.get_array_length(color_table).unwrap() as usize;
    let auto_color_table = env
        .get_auto_primitive_array_critical(color_table, ReleaseMode::CopyBack)
        .unwrap();
    let color_table =
        std::slice::from_raw_parts(auto_color_table.as_ptr() as *const u32, color_table_length);

    fill_pixel(
        &mut pixels,
        &scratch,
        &color_table,
        transparent_color_index as usize,
        image_width as u32,
        frame_width as u32,
        frame_height as u32,
        offset_x as u32,
        offset_y as u32,
        interlaced == JNI_TRUE,
    )
}

fn decode(image_data: &[u8], destination: &mut [u8], pixel_count: usize) {
    let mut prefix: [i16; MAX_STACK_SIZE] = [0; MAX_STACK_SIZE];
    let mut suffix: [u8; MAX_STACK_SIZE] = [0; MAX_STACK_SIZE];
    let mut pixel_stack: [u8; MAX_STACK_SIZE + 1] = [0; MAX_STACK_SIZE + 1];

    let mut data_index = 0;

    let lzw_minimum_code_size = image_data[data_index];
    data_index += 1;

    let clear: u32 = 1 << lzw_minimum_code_size;
    let end_of_data: u32 = clear + 1;
    let mut code_size: u32 = lzw_minimum_code_size as u32 + 1;

    let mut bits = 0;
    let mut current_byte: u32 = 0;
    let mut block_size: u32 = 0;
    let mut mask = (1 << code_size) - 1; // For codeSize = 3, will output 0b0111

    let mut available = clear + 2;
    let mut stack_top = 0;
    let mut first = 0;

    let mut old_code: Option<u32> = None;

    for code in 0..clear {
        prefix[code as usize] = 0;
        suffix[code as usize] = code as u8;
    }
    let mut pixel_index = 0;

    while pixel_index < pixel_count {
        // Getting the next code
        while bits < code_size {
            if block_size == 0 {
                block_size = image_data[data_index] as u32 & 0xff;
                data_index += 1;
            }
            current_byte += (image_data[data_index] as u32 & 0xff) << bits;
            data_index += 1;
            bits += 8;
            block_size -= 1;
        }

        let mut code = current_byte & mask;
        bits -= code_size;
        current_byte = current_byte >> code_size;

        // Interpreting the code
        if code == clear {
            code_size = lzw_minimum_code_size as u32 + 1;
            mask = (1 << code_size) - 1;
            available = clear + 2;
            old_code = None;
            continue;
        } else if code > available || code == end_of_data {
            break;
        } else if old_code == None {
            destination[pixel_index] = suffix[code as usize];
            pixel_index += 1;
            old_code = Some(code);
            first = code;
            continue;
        }

        let initial_code = code;
        if code >= available {
            pixel_stack[stack_top] = first as u8;
            stack_top += 1;
            code = old_code.unwrap();
        }

        while code >= clear {
            pixel_stack[stack_top] = suffix[code as usize];
            stack_top += 1;
            code = prefix[code as usize] as u32 & 0xffff
        }

        first = suffix[code as usize] as u32 & 0xff;

        destination[pixel_index] = first as u8;
        pixel_index += 1;

        while stack_top > 0 {
            stack_top -= 1;
            destination[pixel_index] = pixel_stack[stack_top];
            pixel_index += 1;
        }

        if available < MAX_STACK_SIZE as u32 {
            prefix[available as usize] = old_code.unwrap() as i16;
            suffix[available as usize] = first as u8;
            available += 1;
            if available & mask == 0 && available < MAX_STACK_SIZE as u32 {
                code_size += 1;
                mask += available;
            }
        }
        old_code = Some(initial_code);
    }
}

fn fill_pixel(
    pixels: &mut [u32],
    color_data: &[u8],
    color_table: &[u32],
    transparent_color_index: usize,
    image_width: u32,
    frame_width: u32,
    frame_height: u32,
    offset_x: u32,
    offset_y: u32,
    interlaced: bool,
) {
    if interlaced {
        fill_pixels_interlaced(
            pixels,
            color_data,
            color_table,
            transparent_color_index,
            image_width,
            frame_width,
            frame_height,
            offset_x,
            offset_y,
        )
    } else {
        fill_pixels_simple(
            pixels,
            color_data,
            color_table,
            transparent_color_index,
            image_width,
            frame_width,
            frame_height,
            offset_x,
            offset_y,
        )
    }
}

fn fill_pixels_simple(
    pixels: &mut [u32],
    color_data: &[u8],
    color_table: &[u32],
    transparent_color_index: usize,
    image_width: u32,
    frame_width: u32,
    frame_height: u32,
    offset_x: u32,
    offset_y: u32,
) {
    let size = (frame_width * frame_height) as usize;

    for index in 0..size {
        let color_index = color_data[index] as usize & 0xff;
        if color_index != transparent_color_index {
            let color = color_table[color_index];
            let x = index % frame_width as usize;
            let y = index / frame_width as usize;
            let pixel_index =
                (y + offset_y as usize) * image_width as usize + offset_x as usize + x;
            pixels[pixel_index] = color;
        }
    }
}

fn fill_pixels_interlaced(
    pixels: &mut [u32],
    color_data: &[u8],
    color_table: &[u32],
    transparent_color_index: usize,
    image_width: u32,
    frame_width: u32,
    frame_height: u32,
    offset_x: u32,
    offset_y: u32,
) {
    let mut pass = 0;
    let mut stride = 8;
    let mut matched_line = 0;

    let mut line_index = 0;
    while pass < 4 {
        while matched_line < frame_height {
            let copy_from_index = (line_index * frame_width) as usize;
            let copy_to_index = ((matched_line + offset_y) * image_width + offset_x) as usize;
            let index_offset = copy_to_index - copy_from_index;

            for index in copy_from_index..(copy_from_index + frame_width as usize) {
                let color_index = color_data[index] as usize & 0xff;
                if color_index != transparent_color_index {
                    let color = color_table[color_index];

                    let pixel_index = index + index_offset;
                    pixels[pixel_index] = color;
                }
            }

            line_index += 1;
            matched_line += stride;
        }

        pass += 1;

        match pass {
            1 => {
                matched_line = 4;
                stride = 8;
            }
            2 => {
                matched_line = 2;
                stride = 4;
            }
            3 => {
                matched_line = 1;
                stride = 2;
            }
            _ => {}
        }
    }
}

#[cfg(test)]
mod tests {
    use crate::decode;

    #[test]
    fn test() {
        let sample_data: [u8; 7] = [
            // Initial code size 2
            0b00000010, // Length  5
            0b00000101, // Data
            0b10000100, 0b01101110, 0b00100111, 0b11000001, 0b01011101,
        ];

        let mut destination: [u8; 15] = [0; 15];
        decode(&sample_data, &mut destination, 15)
    }
}
