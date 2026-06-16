use dolby_vision::rpu::dovi_rpu::DoviRpu;
use jni::objects::{JByteArray, JClass};
use jni::sys::{jbyteArray, jint, jintArray};
use jni::JNIEnv;

use dolby_vision::rpu::rpu_data_nlq::DoviELType;

/// Bindings for dovi_convert_nalu_to_p8
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_suyashbelekar_exoplayerhdrutils_libdovi_LibDovi_convertNaluToP8<
    'local,
>(
    env: JNIEnv<'local>,
    _class: JClass<'local>,
    rpu_bytes: JByteArray<'local>,
) -> jbyteArray {
    let rpu_data = match env.convert_byte_array(&rpu_bytes) {
        Ok(bytes) => bytes,
        Err(_) => return std::ptr::null_mut(),
    };

    let mut rpu = match DoviRpu::parse_unspec62_nalu(&rpu_data) {
        Ok(rpu) => rpu,
        Err(_) => return std::ptr::null_mut(),
    };

    if rpu.convert_with_mode(2).is_err() {
        return std::ptr::null_mut();
    }

    match rpu.write_hevc_unspec62_nalu() {
        Ok(bytes) => match env.byte_array_from_slice(&bytes) {
            Ok(java_array) => java_array.into_raw(),
            Err(_) => std::ptr::null_mut(),
        },
        Err(_) => std::ptr::null_mut(),
    }
}

/// Bindings for get_dovi_info
#[unsafe(no_mangle)]
pub extern "system" fn Java_com_suyashbelekar_exoplayerhdrutils_libdovi_LibDovi_getDoviInfo<
    'local,
>(
    env: JNIEnv<'local>,
    _class: JClass<'local>,
    rpu_bytes: JByteArray<'local>,
) -> jintArray {
    let rpu_data = match env.convert_byte_array(&rpu_bytes) {
        Ok(bytes) => bytes,
        Err(_) => return std::ptr::null_mut(),
    };

    let rpu = match DoviRpu::parse_unspec62_nalu(&rpu_data) {
        Ok(rpu) => rpu,
        Err(_) => return std::ptr::null_mut(),
    };

    let profile = rpu.dovi_profile as jint;

    let el_type = if profile == 7 {
        match rpu.el_type {
            Some(DoviELType::FEL) => 2,
            Some(DoviELType::MEL) => 1,
            _ => 0,
        }
    } else {
        0
    };

    // Create a new jintArray of size 2
    let result_array = match env.new_int_array(2) {
        Ok(array) => array,
        Err(_) => {
            env.exception_clear().unwrap_or(());
            return std::ptr::null_mut();
        }
    };

    // Populate the array with [profile, el_type]
    let values: [jint; 2] = [profile, el_type];
    match env.set_int_array_region(&result_array, 0, &values) {
        Ok(_) => result_array.into_raw(),
        Err(_) => {
            env.exception_clear().unwrap_or(());
            std::ptr::null_mut()
        }
    }
}
