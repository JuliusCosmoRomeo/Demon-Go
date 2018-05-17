package com.github.demongo.opencv;

public class Transitor {
    /*private static final String URL = "http://172.18.1.79:5000";

    private String matToBase64String(Mat mat) {
        Bitmap bmp = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bmp);

        // TODO: convert directly to jpeg
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.JPEG, 92, stream);
        byte[] imageBytes = stream.toByteArray();
        String encodedImage = Base64.encodeToString(imageBytes, Base64.DEFAULT);
        return encodedImage;
    }


    private void sendImage(final Mat mat) {
        String url = URL + "/post";

        RequestQueue queue = Volley.newRequestQueue(this);

        StringRequest request = new StringRequest(Request.Method.POST, url, new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.i(TAG, response);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e(TAG,  error.toString());
            }
        }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> parameters = new HashMap<String, String>();
                parameters.put("image", matToBase64String(mat));
                return parameters;
            }
        };

        queue.add(request);
        Log.e(TAG, "POST-request added");
    }*/
}
