use dolby_vision::rpu::dovi_rpu::DoviRpu;
use jni::objects::{JByteArray, JByteBuffer, JClass, JIntArray};
use jni::sys::{jboolean, jbyteArray, jint, jintArray, jlong, jstring};
use jni::JNIEnv;

use dolby_vision::rpu::rpu_data_nlq::DoviELType;

/// Bindings for dovi_parse_unspec62_nalu
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_suyashbelekar_exoplayerhdrutils_libdovi_LibDovi_parseUnspec62Nalu(
    env: JNIEnv,
    _class: JClass,
    rpu_bytes: JByteArray,
) -> jlong {
    let rpu_data = match env.convert_byte_array(&rpu_bytes) {
        Ok(bytes) => bytes,
        Err(_) => return 0,
    };

    match DoviRpu::parse_unspec62_nalu(&rpu_data) {
        Ok(rpu) => {
            let boxed_rpu = Box::new(rpu);
            Box::into_raw(boxed_rpu) as jlong
        }
        Err(_) => 0,
    }
}

/// Bindings for dovi_convert_rpu_with_mode
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_suyashbelekar_exoplayerhdrutils_libdovi_LibDovi_convertRpuWithMode(
    _env: JNIEnv,
    _class: JClass,
    rpu_ptr: jlong,
    mode: jint,
) -> jboolean {
    if rpu_ptr == 0 {
        return jni::sys::JNI_FALSE;
    }

    let rpu = unsafe { &mut *(rpu_ptr as *mut DoviRpu) };

    match rpu.convert_with_mode(mode as u8) {
        Ok(_) => jni::sys::JNI_TRUE,
        Err(_) => jni::sys::JNI_FALSE,
    }
}

/// Bindings for dovi_write_unspec62_nalu
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_suyashbelekar_exoplayerhdrutils_libdovi_LibDovi_writeUnspec62Nalu<'local>(
    env: JNIEnv<'local>,
    _class: JClass<'local>,
    rpu_ptr: jlong,
) -> jbyteArray {
    if rpu_ptr == 0 {
        return std::ptr::null_mut();
    }

    let rpu = unsafe { &*(rpu_ptr as *mut DoviRpu) };

    match rpu.write_hevc_unspec62_nalu() {
        Ok(bytes) => match env.byte_array_from_slice(&bytes) {
            Ok(java_array) => java_array.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
        Err(_) => std::ptr::null_mut(),
    }
}

/// Bindings for dovi_rpu_free
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_suyashbelekar_exoplayerhdrutils_libdovi_LibDovi_freeRpu(
    _env: JNIEnv,
    _class: JClass,
    rpu_ptr: jlong,
) {
    if rpu_ptr != 0 {
        unsafe {
            let _ = Box::from_raw(rpu_ptr as *mut DoviRpu);
        }
    }
}

/// Bindings to retrieve dovi_profile from the rpu
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_suyashbelekar_exoplayerhdrutils_libdovi_LibDovi_getDoviProfile(
    _env: JNIEnv,
    _class: JClass,
    rpu_ptr: jlong,
) -> jint {
    if rpu_ptr == 0 {
        return -1;
    }

    let rpu = unsafe { &*(rpu_ptr as *mut DoviRpu) };

    rpu.dovi_profile as jint
}

/// Bindings to retrieve el_type from the rpu
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_suyashbelekar_exoplayerhdrutils_libdovi_LibDovi_getElType<'local>(
    env: JNIEnv<'local>,
    _class: JClass<'local>,
    rpu_ptr: jlong,
) -> jstring {
    if rpu_ptr == 0 {
        return std::ptr::null_mut();
    }

    let rpu = unsafe { &*(rpu_ptr as *mut DoviRpu) };

    let el_type_str = match rpu.el_type {
        Some(DoviELType::MEL) => "MEL",
        Some(DoviELType::FEL) => "FEL",
        None => "NONE",
    };

    match env.new_string(el_type_str) {
        Ok(java_string) => java_string.into_raw(),
        Err(_) => {
            env.exception_clear().unwrap();
            std::ptr::null_mut()
        }
    }
}

/// Struct to hold complete information about a found NALU
#[derive(Debug, Clone, Copy)]
pub struct NaluInfo {
    pub start_code_idx: usize,
    pub payload_idx: usize,
    pub end_idx: usize,
    pub nalu_type: u8,
}

/// Helper function to find the next start code and payload start indices.
#[inline]
fn find_start_code(data: &[u8], offset: usize) -> Option<(usize, usize)> {
    let mut i = offset.max(2);
    while i < data.len() - 1 {
        if data[i] == 1 && data[i - 1] == 0 && data[i - 2] == 0 {
            let start_code_idx = if i >= 3 && data[i - 3] == 0 {
                i - 3 // 4-byte start code (0 0 0 1)
            } else {
                i - 2 // 3-byte start code (0 0 1)
            };
            return Some((start_code_idx, i + 1));
        }
        i += 1;
    }
    None
}

/// Scans the provided byte slice starting at `offset` to find the next complete NALU.
#[inline]
pub fn get_next_nalu(data: &[u8], offset: usize) -> Option<NaluInfo> {
    let (start_code_idx, payload_idx) = find_start_code(data, offset)?;

    let nalu_type = (data[payload_idx] & 0x7E) >> 1;

    let end_idx = match find_start_code(data, payload_idx) {
        Some((next_start_code_idx, _)) => next_start_code_idx,
        None => data.len(),
    };

    Some(NaluInfo {
        start_code_idx,
        payload_idx,
        end_idx,
        nalu_type,
    })
}

/// Checks if a given HEVC NALU payload contains HDR10+ metadata.
#[inline]
pub fn is_hdr10plus(nalu_data: &[u8]) -> bool {
    if nalu_data.len() < 10 {
        return false;
    }

    // The HDR10+ ITU-T T.35 Signature
    let hdr10plus_signature = [0xB5, 0x00, 0x3C, 0x00, 0x01, 0x04];

    // Skip the first 2 bytes (the header)
    nalu_data[2..]
        .windows(6)
        .any(|window| window == hdr10plus_signature)
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_suyashbelekar_exoplayerhdrutils_libdovi_LibDovi_getRpuFrameInfo(
    env: JNIEnv,
    _class: JClass,
    frame_data: JByteBuffer,
    frame_size: jint,
) -> jintArray {
    let ptr = env
        .get_direct_buffer_address(&frame_data)
        .expect("Failed to get direct buffer address");

    let capacity = env
        .get_direct_buffer_capacity(&frame_data)
        .expect("Failed to get buffer capacity");

    let slice: &[u8] = unsafe { std::slice::from_raw_parts(ptr, capacity) };

    let valid_len = frame_size as usize;
    let mut offset = 0;

    let mut dovi_profile = 0;
    let mut el_type = 0;
    let mut has_hdr10plus = 0;

    while let Some(nalu) = get_next_nalu(&slice[..valid_len], offset) {
        if nalu.nalu_type == 62 {
            let payload = &slice[nalu.payload_idx..nalu.end_idx];

            let dovi_rpu = match DoviRpu::parse_unspec62_nalu(payload) {
                Ok(rpu) => rpu,
                Err(_) => continue,
            };

            dovi_profile = dovi_rpu.dovi_profile;

            el_type = match dovi_rpu.el_type {
                Some(DoviELType::FEL) => 2,
                Some(DoviELType::MEL) => 1,
                None => 0,
            };
        } else if nalu.nalu_type == 39 {
            let payload = &slice[nalu.payload_idx..nalu.end_idx];

            if is_hdr10plus(payload) {
                has_hdr10plus = 1;
            }
        }

        offset = nalu.end_idx;
    }

    let result_data: [i32; 3] = [dovi_profile as i32, el_type as i32, has_hdr10plus as i32];

    let array: JIntArray = env.new_int_array(3).expect("Failed to create int array");

    env.set_int_array_region(&array, 0, &result_data)
        .expect("Failed to set array region");

    array.into_raw()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_com_suyashbelekar_exoplayerhdrutils_libdovi_LibDovi_processHevcFrame(
    env: JNIEnv,
    _class: JClass,
    frame_data: JByteBuffer,
    frame_size: jint,
    dovi_transform: jint,
    needs_hdr10plus_strip: jboolean,
) -> jint {
    let needs_dovi_conversion = dovi_transform == 1;
    let needs_dovi_strip = dovi_transform == 2;
    let needs_hdr10plus_strip_bool = needs_hdr10plus_strip != 0;

    let ptr = env
        .get_direct_buffer_address(&frame_data)
        .expect("Failed to get direct buffer address");

    let capacity = env
        .get_direct_buffer_capacity(&frame_data)
        .expect("Failed to get buffer capacity");

    let slice: &mut [u8] = unsafe { std::slice::from_raw_parts_mut(ptr, capacity) };

    let mut valid_len = frame_size as usize;
    let mut offset = 0;

    while let Some(nalu) = get_next_nalu(&slice[..valid_len], offset) {
        if nalu.nalu_type == 62 {
            if needs_dovi_conversion {
                let payload = &slice[nalu.payload_idx..nalu.end_idx];
                let new_payload = convert_to_p8(payload);

                let old_len = payload.len();
                let new_len = new_payload.len();

                if new_len > old_len {
                    let diff = new_len as isize - old_len as isize;
                    let dest_start = (nalu.end_idx as isize + diff) as usize;

                    slice.copy_within(nalu.end_idx..valid_len, dest_start);
                    valid_len = (valid_len as isize + diff) as usize;
                } else if new_len < old_len {
                    add_hevc_filler(slice, nalu.payload_idx + new_len, nalu.end_idx);
                }

                slice[nalu.payload_idx..nalu.payload_idx + new_len].copy_from_slice(&new_payload);

                offset = nalu.payload_idx + new_len;
            } else if needs_dovi_strip {
                add_hevc_filler(slice, nalu.payload_idx, nalu.end_idx);
                offset = nalu.end_idx;
            }
        } else if nalu.nalu_type == 39 && needs_hdr10plus_strip_bool {
            add_hevc_filler(slice, nalu.payload_idx, nalu.end_idx);
            offset = nalu.end_idx;
        } else {
            offset = nalu.end_idx;
        }
    }

    valid_len as jint
}

/// Fills the leftover space of a shrunken HEVC NALU with valid padding.
pub fn add_hevc_filler(buffer: &mut [u8], start: usize, end: usize) {
    let gap = end - start;

    if gap < 6 {
        // Annex B Zero-Padding for gaps too small for a NALU header (1 to 5 bytes)
        buffer[start..end].fill(0x00);
    } else {
        // Standard HEVC Filler Data (Type 38) for larger gaps
        buffer[start..start + 4].copy_from_slice(&[0x00, 0x00, 0x00, 0x01]);
        buffer[start + 4..start + 6].copy_from_slice(&[0x4C, 0x01]);

        if gap > 6 {
            buffer[start + 6..end].fill(0xFF);
        }
    }
}

#[inline]
fn convert_to_p8(payload: &[u8]) -> Vec<u8> {
    let mut dovi_rpu = match DoviRpu::parse_unspec62_nalu(payload) {
        Ok(rpu) => rpu,
        Err(_) => return payload.to_vec(),
    };

    if dovi_rpu.convert_with_mode(2).is_err() {
        return payload.to_vec();
    }

    match dovi_rpu.write_hevc_unspec62_nalu() {
        Ok(new_payload) => new_payload,
        Err(_) => payload.to_vec(),
    }
}
